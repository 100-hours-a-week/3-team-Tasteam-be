package com.tasteam.domain.recommendation.importer;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.importer.config.RecommendationImportPollingProperties;
import com.tasteam.infra.storage.StorageClient;

@Component
public class S3RecommendationResultPollingService {

	private static final Pattern DT_PATTERN = Pattern.compile(".*/dt=(\\d{4}-\\d{2}-\\d{2})/.*");

	private final StorageClient storageClient;
	private final RecommendationImportPollingProperties pollingProperties;

	public S3RecommendationResultPollingService(
		StorageClient storageClient,
		RecommendationImportPollingProperties pollingProperties) {
		this.storageClient = storageClient;
		this.pollingProperties = pollingProperties;
	}

	public RecommendationResultS3Target awaitImportTarget(String s3PrefixOrUri, String pipelineVersion,
		String requestId) {
		S3Location location = parseS3Uri(s3PrefixOrUri);
		String pipelinePrefix = resolvePipelinePrefix(location.key(), pipelineVersion);
		Duration timeout = defaultIfNonPositive(pollingProperties.getTimeout(), Duration.ofMinutes(10));
		Duration interval = defaultIfNonPositive(pollingProperties.getInterval(), Duration.ofSeconds(10));
		Instant deadline = Instant.now().plus(timeout);

		while (!Instant.now().isAfter(deadline)) {
			RecommendationResultS3Target found = findLatestCompletedTarget(location.bucket(), pipelinePrefix,
				pipelineVersion);
			if (found != null) {
				return found;
			}
			sleep(interval);
		}
		throw RecommendationBusinessException.resultPollingTimeout(
			"S3 추천 결과 대기 시간 초과. requestId=" + requestId + ", prefix=" + s3PrefixOrUri
				+ ", pipelineVersion=" + pipelineVersion);
	}

	private RecommendationResultS3Target findLatestCompletedTarget(
		String bucket,
		String pipelinePrefix,
		String pipelineVersion) {

		Map<LocalDate, RecommendationBatchFolderState> states = collectBatchStates(pipelinePrefix);
		return states.entrySet().stream()
			.filter(entry -> entry.getValue().ready())
			.max(Map.Entry.comparingByKey())
			.map(entry -> new RecommendationResultS3Target(
				"s3://" + bucket + "/" + entry.getValue().firstCsvKey(),
				pipelineVersion,
				entry.getKey()))
			.orElse(null);
	}

	private Map<LocalDate, RecommendationBatchFolderState> collectBatchStates(String pipelinePrefix) {
		Map<LocalDate, RecommendationBatchFolderState> states = new HashMap<>();
		for (String key : storageClient.listObjects(pipelinePrefix)) {
			LocalDate dt = extractBatchDate(key);
			if (dt == null) {
				continue;
			}
			RecommendationBatchFolderState state = states.computeIfAbsent(
				dt,
				ignored -> new RecommendationBatchFolderState());
			if (key.endsWith("/_SUCCESS")) {
				state.markSuccess();
				continue;
			}
			if (key.endsWith(".csv")) {
				state.addCsvKey(key);
			}
		}

		return states;
	}

	private LocalDate extractBatchDate(String key) {
		if (!StringUtils.hasText(key)) {
			return null;
		}
		Matcher matcher = DT_PATTERN.matcher(key);
		if (!matcher.matches()) {
			return null;
		}
		return LocalDate.parse(matcher.group(1));
	}

	private String resolvePipelinePrefix(String baseKey, String pipelineVersion) {
		String normalized = normalizePrefix(baseKey);
		String marker = "pipeline_version=" + pipelineVersion + "/";
		if (normalized.contains(marker)) {
			return normalized;
		}
		return normalized + marker;
	}

	private String normalizePrefix(String key) {
		String normalized = key.startsWith("/") ? key.substring(1) : key;
		if (!normalized.endsWith("/")) {
			normalized = normalized + "/";
		}
		return normalized;
	}

	private Duration defaultIfNonPositive(Duration value, Duration defaultValue) {
		if (value == null || value.isNegative() || value.isZero()) {
			return defaultValue;
		}
		return value;
	}

	private void sleep(Duration interval) {
		try {
			Thread.sleep(interval.toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw RecommendationBusinessException.resultPollingTimeout("S3 추천 결과 대기 중 인터럽트가 발생했습니다.");
		}
	}

	private S3Location parseS3Uri(String s3Uri) {
		if (!StringUtils.hasText(s3Uri) || !s3Uri.startsWith("s3://")) {
			throw RecommendationBusinessException.resultValidationFailed("S3 경로 형식이 올바르지 않습니다: " + s3Uri);
		}
		String remainder = s3Uri.substring("s3://".length());
		int slashIndex = remainder.indexOf('/');
		if (slashIndex <= 0 || slashIndex == remainder.length() - 1) {
			throw RecommendationBusinessException.resultValidationFailed("S3 경로 형식이 올바르지 않습니다: " + s3Uri);
		}
		return new S3Location(remainder.substring(0, slashIndex), remainder.substring(slashIndex + 1));
	}

	private static final class RecommendationBatchFolderState {

		private boolean successMarkerExists;
		private final List<String> csvKeys = new ArrayList<>();

		private void markSuccess() {
			successMarkerExists = true;
		}

		private void addCsvKey(String csvKey) {
			csvKeys.add(csvKey);
		}

		private boolean ready() {
			return successMarkerExists && !csvKeys.isEmpty();
		}

		private String firstCsvKey() {
			return csvKeys.stream()
				.sorted(Comparator.naturalOrder())
				.findFirst()
				.orElseThrow(() -> RecommendationBusinessException.resultValidationFailed("CSV 결과 파일이 존재하지 않습니다."));
		}
	}

	private record S3Location(String bucket, String key) {
	}
}
