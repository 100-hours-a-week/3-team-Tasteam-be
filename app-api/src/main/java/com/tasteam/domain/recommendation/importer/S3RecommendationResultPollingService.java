package com.tasteam.domain.recommendation.importer;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.importer.config.RecommendationImportPollingProperties;

@Component
public class S3RecommendationResultPollingService {

	private final AmazonS3 amazonS3;
	private final RecommendationImportPollingProperties pollingProperties;

	public S3RecommendationResultPollingService(
		AmazonS3 amazonS3,
		RecommendationImportPollingProperties pollingProperties) {
		this.amazonS3 = amazonS3;
		this.pollingProperties = pollingProperties;
	}

	public String awaitResultS3Uri(String s3PrefixOrUri, String requestId) {
		S3Location location = parseS3Uri(s3PrefixOrUri);
		Duration timeout = defaultIfNonPositive(pollingProperties.getTimeout(), Duration.ofMinutes(10));
		Duration interval = defaultIfNonPositive(pollingProperties.getInterval(), Duration.ofSeconds(10));
		Instant deadline = Instant.now().plus(timeout);

		while (!Instant.now().isAfter(deadline)) {
			String found = findFirstCsvUri(location);
			if (found != null) {
				return found;
			}
			sleep(interval);
		}
		throw RecommendationBusinessException.resultPollingTimeout(
			"S3 추천 결과 대기 시간 초과. requestId=" + requestId + ", prefix=" + s3PrefixOrUri);
	}

	private Duration defaultIfNonPositive(Duration value, Duration defaultValue) {
		if (value == null || value.isNegative() || value.isZero()) {
			return defaultValue;
		}
		return value;
	}

	private String findFirstCsvUri(S3Location location) {
		ListObjectsV2Request req = new ListObjectsV2Request()
			.withBucketName(location.bucket())
			.withPrefix(location.key())
			.withMaxKeys(200);
		List<S3ObjectSummary> summaries = amazonS3.listObjectsV2(req).getObjectSummaries();

		return summaries.stream()
			.map(S3ObjectSummary::getKey)
			.filter(StringUtils::hasText)
			.filter(key -> key.endsWith(".csv"))
			.sorted(Comparator.naturalOrder())
			.map(key -> "s3://" + location.bucket() + "/" + key)
			.findFirst()
			.orElse(null);
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

	private record S3Location(String bucket, String key) {
	}
}
