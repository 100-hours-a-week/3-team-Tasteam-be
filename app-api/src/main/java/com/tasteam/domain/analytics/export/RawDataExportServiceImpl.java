package com.tasteam.domain.analytics.export;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.tasteam.domain.analytics.config.AnalyticsProperties;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;
import com.tasteam.infra.storage.StorageClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RawDataExportServiceImpl implements RawDataExportService {

	private static final String BASE_PREFIX = "raw";
	// TODO(recommendation-pipeline): 현재는 dt 기준 full-snapshot REPLACE 방식.
	// 증분 적재가 필요해지면 source 쿼리를 updated_at/변경 이벤트 기준으로 분리하고
	// append 가능한 파티셔닝/중복제거 전략을 함께 설계해 전환한다.
	// TODO: 대용량 대응 시 row chunk 기반 part-00001~N 멀티파일 업로드로 확장 (현재는 dt/type 당 단일 스냅샷 파일 1개만 업로드)
	private static final String DATA_FILE_NAME = "part-00001.csv";
	private static final String SUCCESS_FILE_NAME = "_SUCCESS";
	private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
	private static final BatchType EXPORT_BATCH_TYPE = BatchType.RAW_DATA_EXPORT;

	private final RawDataExportSourceJdbcRepository sourceRepository;
	private final StorageClient storageClient;
	private final BatchExecutionRepository batchExecutionRepository;
	private String analyticsBucket;
	private RawDataExportRuntimeDiagnostics runtimeDiagnostics = RawDataExportRuntimeDiagnostics.noop();

	public RawDataExportServiceImpl(
		RawDataExportSourceJdbcRepository sourceRepository,
		StorageClient storageClient,
		BatchExecutionRepository batchExecutionRepository) {
		this.sourceRepository = sourceRepository;
		this.storageClient = storageClient;
		this.batchExecutionRepository = batchExecutionRepository;
	}

	@Autowired(required = false)
	void setRuntimeDiagnostics(RawDataExportRuntimeDiagnostics runtimeDiagnostics) {
		if (runtimeDiagnostics != null) {
			this.runtimeDiagnostics = runtimeDiagnostics;
		}
	}

	@Autowired(required = false)
	void setAnalyticsProperties(AnalyticsProperties analyticsProperties) {
		if (analyticsProperties != null && StringUtils.hasText(analyticsProperties.getBucket())) {
			this.analyticsBucket = analyticsProperties.getBucket();
			log.info("analytics export bucket resolved. bucket={}", analyticsBucket);
		}
	}

	@Override
	public RawDataExportResult export(RawDataExportCommand command) {
		Assert.notNull(command, "command는 null일 수 없습니다.");
		LocalDate dt = command.dt() == null ? LocalDate.now(KST_ZONE) : command.dt();
		Set<RawDataType> targets = command.targets() == null || command.targets().isEmpty()
			? EnumSet.allOf(RawDataType.class)
			: EnumSet.copyOf(command.targets());

		BatchExecution execution = startBatchExecution();
		List<RawDataExportItemResult> items = new ArrayList<>();
		int successCount = 0;
		int totalJobs = targets.size();
		runtimeDiagnostics.logSnapshot("start", command, dt, totalJobs, successCount);
		try {
			for (RawDataType type : RawDataType.values()) {
				if (!targets.contains(type)) {
					continue;
				}
				RawDataCsvTable table = extract(type);
				items.add(upload(type, dt, table, command.requestId()));
				successCount += 1;
			}
			finishBatchExecution(execution, totalJobs, successCount, BatchExecutionStatus.COMPLETED);
			runtimeDiagnostics.logSnapshot("completed", command, dt, totalJobs, successCount);
			return new RawDataExportResult(dt, List.copyOf(items));
		} catch (RuntimeException ex) {
			finishBatchExecution(execution, totalJobs, successCount, BatchExecutionStatus.FAILED);
			runtimeDiagnostics.logSnapshot("failed", command, dt, totalJobs, successCount);
			log.error("raw data export failed. batchExecutionId={}, requestId={}, dt={}, totalJobs={}, successCount={}",
				execution.getId(), command.requestId(), dt, totalJobs, successCount, ex);
			throw ex;
		}
	}

	private RawDataCsvTable extract(RawDataType type) {
		return switch (type) {
			case RESTAURANTS -> sourceRepository.extractRestaurants();
			case MENUS -> sourceRepository.extractMenus();
		};
	}

	private RawDataExportItemResult upload(RawDataType type, LocalDate dt, RawDataCsvTable table, String requestId) {
		String prefix = BASE_PREFIX + "/" + type.pathSegment() + "/dt=" + dt + "/";
		String dataObjectKey = prefix + DATA_FILE_NAME;
		String successObjectKey = prefix + SUCCESS_FILE_NAME;
		List<String> existingObjects = listObjects(prefix);
		String csv = toCsv(table);

		try {
			// 동일 dt 재실행(REPLACE) 시 AI 완료 판정 파일이 남아 있지 않도록 먼저 제거한다.
			deleteObject(successObjectKey);
			// 스냅샷 CSV는 동일 키에 overwrite 한다.
			uploadObject(dataObjectKey, csv.getBytes(StandardCharsets.UTF_8), "text/csv");
			// 데이터 파일 업로드가 끝난 뒤 완료 마커를 생성한다.
			uploadObject(successObjectKey, new byte[0], "text/plain");
		} catch (RuntimeException ex) {
			// 실패 시 미완료 데이터를 완료로 오인하지 않도록 _SUCCESS를 정리한다.
			safeDeleteSuccessMarker(successObjectKey);
			throw ex;
		}

		log.info(
			"raw data export completed. requestId={}, type={}, dt={}, rowCount={}, dataKey={}, replacedExisting={}",
			requestId,
			type,
			dt,
			table.rowCount(),
			dataObjectKey,
			!existingObjects.isEmpty());

		return new RawDataExportItemResult(
			type,
			table.rowCount(),
			dataObjectKey,
			successObjectKey,
			!existingObjects.isEmpty());
	}

	private String toCsv(RawDataCsvTable table) {
		StringBuilder sb = new StringBuilder();
		sb.append(join(table.headers())).append('\n');
		for (List<String> row : table.rows()) {
			sb.append(join(row)).append('\n');
		}
		return sb.toString();
	}

	private String join(List<String> fields) {
		List<String> escaped = new ArrayList<>(fields.size());
		for (String field : fields) {
			escaped.add(escape(field));
		}
		return String.join(",", escaped);
	}

	private String escape(String value) {
		if (value == null) {
			return "";
		}
		boolean shouldQuote = value.contains(",") || value.contains("\"") || value.contains("\n")
			|| value.contains("\r");
		if (!shouldQuote) {
			return value;
		}
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}

	private BatchExecution startBatchExecution() {
		BatchExecution execution = BatchExecution.start(EXPORT_BATCH_TYPE, Instant.now());
		return batchExecutionRepository.save(execution);
	}

	private void safeDeleteSuccessMarker(String successObjectKey) {
		try {
			deleteObject(successObjectKey);
		} catch (RuntimeException ignore) {
			log.warn("failed to cleanup success marker. key={}", successObjectKey, ignore);
		}
	}

	private List<String> listObjects(String prefix) {
		if (StringUtils.hasText(analyticsBucket)) {
			return storageClient.listObjects(analyticsBucket, prefix);
		}
		return storageClient.listObjects(prefix);
	}

	private void deleteObject(String objectKey) {
		if (StringUtils.hasText(analyticsBucket)) {
			storageClient.deleteObject(analyticsBucket, objectKey);
			return;
		}
		storageClient.deleteObject(objectKey);
	}

	private void uploadObject(String objectKey, byte[] data, String contentType) {
		if (StringUtils.hasText(analyticsBucket)) {
			storageClient.uploadObject(analyticsBucket, objectKey, data, contentType);
			return;
		}
		storageClient.uploadObject(objectKey, data, contentType);
	}

	private void finishBatchExecution(BatchExecution execution, int totalJobs, int successCount,
		BatchExecutionStatus finalStatus) {
		int failedCount = Math.max(0, totalJobs - successCount);
		execution.finish(
			Instant.now(),
			totalJobs,
			successCount,
			failedCount,
			0,
			finalStatus);
		batchExecutionRepository.save(execution);
	}
}
