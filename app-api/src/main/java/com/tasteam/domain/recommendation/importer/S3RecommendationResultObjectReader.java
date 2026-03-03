package com.tasteam.domain.recommendation.importer;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;

@Component
public class S3RecommendationResultObjectReader implements RecommendationResultObjectReader {

	private final AmazonS3 amazonS3;

	public S3RecommendationResultObjectReader(AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
	}

	@Override
	public java.io.InputStream openStream(String s3Uri) {
		S3Location location = parseS3Uri(s3Uri);
		S3Object object = amazonS3.getObject(location.bucket(), location.key());
		return object.getObjectContent();
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
		String bucket = remainder.substring(0, slashIndex);
		String key = remainder.substring(slashIndex + 1);
		return new S3Location(bucket, key);
	}

	private record S3Location(String bucket, String key) {
	}
}
