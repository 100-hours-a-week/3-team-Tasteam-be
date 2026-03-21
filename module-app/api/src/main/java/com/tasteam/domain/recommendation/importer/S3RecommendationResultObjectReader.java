package com.tasteam.domain.recommendation.importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.domain.analytics.config.AnalyticsProperties;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.infra.storage.StorageClient;

@Component
public class S3RecommendationResultObjectReader implements RecommendationResultObjectReader {

	private final StorageClient storageClient;
	private String analyticsBucket;

	public S3RecommendationResultObjectReader(StorageClient storageClient) {
		this.storageClient = storageClient;
	}

	@Autowired(required = false)
	void setAnalyticsProperties(AnalyticsProperties analyticsProperties) {
		if (analyticsProperties != null && StringUtils.hasText(analyticsProperties.getBucket())) {
			this.analyticsBucket = analyticsProperties.getBucket();
		}
	}

	@Override
	public java.io.InputStream openStream(String s3Uri) {
		S3Location location = parseS3Uri(s3Uri);
		byte[] data = storageClient.downloadObject(resolveAnalyticsBucket(), location.key());
		return new java.io.ByteArrayInputStream(data);
	}

	private String resolveAnalyticsBucket() {
		if (!StringUtils.hasText(analyticsBucket)) {
			throw RecommendationBusinessException.resultValidationFailed(
				"analytics bucket 설정이 비어 있습니다. tasteam.analytics.bucket 값을 확인해 주세요.");
		}
		return analyticsBucket;
	}

	private S3Location parseS3Uri(String s3Uri) {
		if (!StringUtils.hasText(s3Uri) || !s3Uri.startsWith("s3://")) {
			throw RecommendationBusinessException.resultValidationFailed("s3Uri 형식이 올바르지 않습니다: " + s3Uri);
		}
		String remainder = s3Uri.substring("s3://".length());
		int slashIndex = remainder.indexOf('/');
		if (slashIndex <= 0 || slashIndex == remainder.length() - 1) {
			throw RecommendationBusinessException.resultValidationFailed("s3Uri 형식이 올바르지 않습니다: " + s3Uri);
		}
		String key = remainder.substring(slashIndex + 1);
		return new S3Location(key);
	}

	private record S3Location(String key) {
	}
}
