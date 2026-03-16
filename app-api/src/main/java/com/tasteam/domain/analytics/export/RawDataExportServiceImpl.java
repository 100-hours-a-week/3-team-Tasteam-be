package com.tasteam.domain.analytics.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.tasteam.domain.analytics.config.AnalyticsProperties;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;
import com.tasteam.global.metrics.MetricLabelPolicy;
import com.tasteam.infra.storage.StorageClient;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
	private final MeterRegistry meterRegistry;
	private String analyticsBucket;
	private RawDataExportRuntimeDiagnostics runtimeDiagnostics = RawDataExportRuntimeDiagnostics.noop();

	public RawDataExportServiceImpl(
		RawDataExportSourceJdbcRepository sourceRepository,
		StorageClient storageClient,
		BatchExecutionRepository batchExecutionRepository,
		MeterRegistry meterRegistry) {
		this.sourceRepository = sourceRepository;
		this.storageClient = storageClient;
		this.batchExecutionRepository = batchExecutionRepository;
		this.meterRegistry = meterRegistry;
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
		Timer.Sample totalSample = Timer.start(meterRegistry);
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
				items.add(upload(type, dt, command.requestId()));
				successCount += 1;
			}
			finishBatchExecution(execution, totalJobs, successCount, BatchExecutionStatus.COMPLETED);
			runtimeDiagnostics.logSnapshot("completed", command, dt, totalJobs, successCount);
			recordCounter("analytics.raw_export.execute.total", "stage", "total", "result", "success");
			recordTimer("analytics.raw_export.execute.duration", totalSample, "stage", "total", "result", "success");
			return new RawDataExportResult(dt, List.copyOf(items));
		} catch (RuntimeException ex) {
			finishBatchExecution(execution, totalJobs, successCount, BatchExecutionStatus.FAILED);
			runtimeDiagnostics.logSnapshot("failed", command, dt, totalJobs, successCount);
			log.error("raw data export failed. batchExecutionId={}, requestId={}, dt={}, totalJobs={}, successCount={}",
				execution.getId(), command.requestId(), dt, totalJobs, successCount, ex);
			recordCounter("analytics.raw_export.execute.total", "stage", "total", "result", "failed");
			recordTimer("analytics.raw_export.execute.duration", totalSample, "stage", "total", "result", "failed");
			throw ex;
		}
	}

	private RawDataExportItemResult upload(RawDataType type, LocalDate dt, String requestId) {
		Timer.Sample typeSample = Timer.start(meterRegistry);
		String typeTag = type.pathSegment();
		String prefix = BASE_PREFIX + "/" + type.pathSegment() + "/dt=" + dt + "/";
		String dataObjectKey = prefix + DATA_FILE_NAME;
		String successObjectKey = prefix + SUCCESS_FILE_NAME;
		List<String> existingObjects = listObjects(prefix);
		CsvPayload csvPayload = buildCsvFile(type);

		try {
			// 동일 dt 재실행(REPLACE) 시 AI 완료 판정 파일이 남아 있지 않도록 먼저 제거한다.
			deleteObject(successObjectKey);
			// 1차는 단일 파일 업로드만 수행한다(멀티파트/분할 업로드는 2차 작업에서 도입).
			uploadObject(dataObjectKey, csvPayload.csvFile(), "text/csv");
			// 데이터 파일 업로드가 끝난 뒤 완료 마커를 생성한다.
			uploadObject(successObjectKey, new byte[0], "text/plain");
		} catch (RuntimeException ex) {
			// 실패 시 미완료 데이터를 완료로 오인하지 않도록 _SUCCESS를 정리한다.
			safeDeleteSuccessMarker(successObjectKey);
			recordCounter("analytics.raw_export.execute.total", "stage", typeTag, "result", "failed");
			recordTimer("analytics.raw_export.execute.duration", typeSample, "stage", typeTag, "result", "failed");
			throw ex;
		} finally {
			safeDeleteTempFile(csvPayload.csvFile());
		}

		log.info(
			"raw data export completed. requestId={}, type={}, dt={}, rowCount={}, dataKey={}, replacedExisting={}",
			requestId,
			type,
			dt,
			csvPayload.rowCount(),
			dataObjectKey,
			!existingObjects.isEmpty());
		recordRows(csvPayload.rowCount(), typeTag);
		recordCounter("analytics.raw_export.execute.total", "stage", typeTag, "result", "success");
		recordTimer("analytics.raw_export.execute.duration", typeSample, "stage", typeTag, "result", "success");

		return new RawDataExportItemResult(
			type,
			csvPayload.rowCount(),
			dataObjectKey,
			successObjectKey,
			!existingObjects.isEmpty());
	}

	private CsvPayload buildCsvFile(RawDataType type) {
		AtomicInteger rowCount = new AtomicInteger(0);
		Path tempFile = createTempCsvFile(type);
		try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
			writeRow(writer, headers(type));
			streamRows(type, row -> {
				try {
					writeRow(writer, row);
				} catch (IOException e) {
					throw new IllegalStateException("failed to write csv row", e);
				}
				rowCount.incrementAndGet();
			});
			writer.flush();
		} catch (IOException e) {
			throw new IllegalStateException("failed to build csv payload", e);
		}
		return new CsvPayload(tempFile, rowCount.get());
	}

	private List<String> headers(RawDataType type) {
		return switch (type) {
			case RESTAURANTS -> sourceRepository.restaurantHeaders();
			case MENUS -> sourceRepository.menuHeaders();
		};
	}

	private void streamRows(RawDataType type, CsvRowConsumer consumer) {
		switch (type) {
			case RESTAURANTS -> sourceRepository.streamRestaurants(consumer);
			case MENUS -> sourceRepository.streamMenus(consumer);
		}
	}

	private String join(List<String> fields) {
		List<String> escaped = new ArrayList<>(fields.size());
		for (String field : fields) {
			escaped.add(escape(field));
		}
		return String.join(",", escaped);
	}

	private void writeRow(BufferedWriter writer, List<String> fields) throws IOException {
		writer.write(join(fields));
		writer.newLine();
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

	private Path createTempCsvFile(RawDataType type) {
		try {
			return Files.createTempFile("raw-" + type.pathSegment() + "-", ".csv");
		} catch (IOException e) {
			throw new IllegalStateException("failed to create temp csv file", e);
		}
	}

	private void safeDeleteTempFile(Path tempFile) {
		if (tempFile == null) {
			return;
		}
		try {
			Files.deleteIfExists(tempFile);
		} catch (IOException e) {
			log.warn("failed to delete temp csv file. path={}", tempFile, e);
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

	private void uploadObject(String objectKey, Path file, String contentType) {
		if (StringUtils.hasText(analyticsBucket)) {
			storageClient.uploadObject(analyticsBucket, objectKey, file, contentType);
			return;
		}
		storageClient.uploadObject(objectKey, file, contentType);
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

	private record CsvPayload(Path csvFile, int rowCount) {
	}

	private void recordRows(int rowCount, String stage) {
		String metricName = "analytics.raw_export.rows";
		MetricLabelPolicy.validate(metricName, "stage", stage);
		DistributionSummary.builder(metricName)
			.tags("stage", stage)
			.register(meterRegistry)
			.record(rowCount);
	}

	private void recordCounter(String metricName, String... tags) {
		MetricLabelPolicy.validate(metricName, tags);
		meterRegistry.counter(metricName, tags).increment();
	}

	private void recordTimer(String metricName, Timer.Sample sample, String... tags) {
		MetricLabelPolicy.validate(metricName, tags);
		sample.stop(Timer.builder(metricName).tags(tags).register(meterRegistry));
	}
}
