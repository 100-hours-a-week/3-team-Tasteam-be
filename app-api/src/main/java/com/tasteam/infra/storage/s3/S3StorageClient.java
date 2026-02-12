package com.tasteam.infra.storage.s3;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Date;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FileErrorCode;
import com.tasteam.infra.storage.PresignedPostRequest;
import com.tasteam.infra.storage.PresignedPostResponse;
import com.tasteam.infra.storage.StorageClient;
import com.tasteam.infra.storage.StorageProperties;
import com.tasteam.infra.storage.s3.policy.S3PresignPolicy;
import com.tasteam.infra.storage.s3.policy.S3PresignPolicyBuilder;

import lombok.RequiredArgsConstructor;

@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "tasteam.storage", name = "type", havingValue = "s3")
@RequiredArgsConstructor
public class S3StorageClient implements StorageClient {

	private final AmazonS3 amazonS3;
	private final AWSCredentialsProvider credentialsProvider;
	private final StorageProperties properties;
	private final S3PresignPolicyBuilder presignPolicyBuilder;

	@Override
	public PresignedPostResponse createPresignedPost(PresignedPostRequest request) {
		try {
			validatePresignedPostRequest(request);
			AWSCredentials credentials = getValidatedCredentials();

			S3PresignPolicy policy = presignPolicyBuilder.build(
				request,
				credentials,
				resolveRegion(),
				resolveBucket(),
				properties.getPresignedExpirationSeconds());

			return new PresignedPostResponse(resolveBaseUrl(), policy.fields(), policy.expiresAt());
		} catch (RuntimeException ex) {
			throw mapStorageException(ex);
		}
	}

	@Override
	public String createPresignedGetUrl(String objectKey) {
		try {
			Assert.hasText(objectKey, "objectKey는 필수입니다");
			Instant expiry = calculateExpiry(properties.getPresignedExpirationSeconds());
			return amazonS3.generatePresignedUrl(
				resolveBucket(),
				objectKey,
				Date.from(expiry),
				HttpMethod.GET).toString();
		} catch (RuntimeException ex) {
			throw mapStorageException(ex);
		}
	}

	@Override
	public void deleteObject(String objectKey) {
		try {
			Assert.hasText(objectKey, "objectKey는 필수입니다");
			amazonS3.deleteObject(new DeleteObjectRequest(resolveBucket(), objectKey));
		} catch (RuntimeException ex) {
			throw mapStorageException(ex);
		}
	}

	@Override
	public byte[] downloadObject(String objectKey) {
		Assert.hasText(objectKey, "objectKey는 필수입니다");
		try (S3Object s3Object = amazonS3.getObject(resolveBucket(), objectKey)) {
			return IOUtils.toByteArray(s3Object.getObjectContent());
		} catch (IOException ex) {
			throw new BusinessException(FileErrorCode.STORAGE_ERROR, ex.getMessage());
		} catch (RuntimeException ex) {
			throw mapStorageException(ex);
		}
	}

	@Override
	public void uploadObject(String objectKey, byte[] data, String contentType) {
		try {
			Assert.hasText(objectKey, "objectKey는 필수입니다");
			Assert.notNull(data, "data는 필수입니다");
			Assert.hasText(contentType, "contentType은 필수입니다");

			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(data.length);
			metadata.setContentType(contentType);

			amazonS3.putObject(new PutObjectRequest(
				resolveBucket(),
				objectKey,
				new java.io.ByteArrayInputStream(data),
				metadata));
		} catch (RuntimeException ex) {
			throw mapStorageException(ex);
		}
	}

	private String resolveBaseUrl() {
		String baseUrl = properties.getBaseUrl();
		if (baseUrl != null && !baseUrl.isBlank()) {
			return baseUrl;
		}
		return String.format("https://%s.s3.%s.amazonaws.com", resolveBucket(), resolveRegion());
	}

	private String resolveBucket() {
		Assert.hasText(properties.getBucket(), "storage.bucket은 필수입니다");
		return properties.getBucket();
	}

	private String resolveRegion() {
		Assert.hasText(properties.getRegion(), "tasteam.storage.region은 필수입니다");
		return properties.getRegion();
	}

	private void validatePresignedPostRequest(PresignedPostRequest request) {
		Assert.notNull(request, "presigned 요청은 필수입니다");
		Assert.hasText(request.objectKey(), "objectKey는 필수입니다");
		Assert.hasText(request.contentType(), "contentType은 필수입니다");
		Assert.isTrue(request.minContentLength() > 0, "minContentLength는 1 이상이어야 합니다");
		Assert.isTrue(request.maxContentLength() >= request.minContentLength(),
			"maxContentLength는 minContentLength 이상이어야 합니다");
	}

	private AWSCredentials getValidatedCredentials() {
		AWSCredentials credentials = credentialsProvider.getCredentials();
		Assert.hasText(credentials.getAWSAccessKeyId(), "AWS access key가 필요합니다");
		Assert.hasText(credentials.getAWSSecretKey(), "AWS secret key가 필요합니다");
		return credentials;
	}

	private Instant calculateExpiry(long expirationSeconds) {
		try {
			return Instant.now().plusSeconds(expirationSeconds);
		} catch (DateTimeException ex) {
			throw new IllegalStateException("presigned 만료 시간을 계산할 수 없습니다", ex);
		}
	}

	private RuntimeException mapStorageException(RuntimeException ex) {
		if (ex instanceof BusinessException businessException) {
			return businessException;
		}
		if (ex instanceof AmazonServiceException serviceException) {
			return mapServiceException(serviceException);
		}
		if (ex instanceof AmazonClientException clientException) {
			return mapClientException(clientException);
		}
		return new BusinessException(FileErrorCode.STORAGE_ERROR, ex.getMessage());
	}

	private RuntimeException mapServiceException(AmazonServiceException ex) {
		String errorCode = ex.getErrorCode();
		if (errorCode == null || errorCode.isBlank()) {
			if (ex.getStatusCode() >= 500) {
				return new BusinessException(FileErrorCode.STORAGE_SERVICE_UNAVAILABLE, ex.getMessage());
			}
			return new BusinessException(FileErrorCode.STORAGE_ERROR, ex.getMessage());
		}
		return switch (errorCode) {
			case "AccessDenied" -> new BusinessException(FileErrorCode.STORAGE_ACCESS_DENIED, ex.getMessage());
			case "InvalidAccessKeyId" ->
				new BusinessException(FileErrorCode.STORAGE_INVALID_CREDENTIALS, ex.getMessage());
			case "SignatureDoesNotMatch" ->
				new BusinessException(FileErrorCode.STORAGE_SIGNATURE_MISMATCH, ex.getMessage());
			case "ExpiredToken", "InvalidToken" ->
				new BusinessException(FileErrorCode.STORAGE_TOKEN_EXPIRED, ex.getMessage());
			case "RequestTimeTooSkewed", "RequestExpired" ->
				new BusinessException(FileErrorCode.STORAGE_REQUEST_EXPIRED, ex.getMessage());
			case "NoSuchBucket" -> new BusinessException(FileErrorCode.STORAGE_BUCKET_NOT_FOUND, ex.getMessage());
			case "NoSuchKey" -> new BusinessException(FileErrorCode.STORAGE_OBJECT_NOT_FOUND, ex.getMessage());
			case "InvalidBucketName", "InvalidBucketState" ->
				new BusinessException(FileErrorCode.STORAGE_BUCKET_INVALID, ex.getMessage());
			case "EntityTooLarge" -> new BusinessException(FileErrorCode.STORAGE_ENTITY_TOO_LARGE, ex.getMessage());
			case "EntityTooSmall" -> new BusinessException(FileErrorCode.STORAGE_ENTITY_TOO_SMALL, ex.getMessage());
			case "InvalidArgument", "InvalidRequest", "InvalidDigest", "BadDigest", "MissingContentLength",
				"InvalidPart", "InvalidPartOrder" ->
				new BusinessException(FileErrorCode.STORAGE_INVALID_REQUEST, ex.getMessage());
			case "SlowDown", "Throttling", "ThrottlingException" ->
				new BusinessException(FileErrorCode.STORAGE_THROTTLED, ex.getMessage());
			case "InternalError" -> new BusinessException(FileErrorCode.STORAGE_INTERNAL_ERROR, ex.getMessage());
			case "ServiceUnavailable" ->
				new BusinessException(FileErrorCode.STORAGE_SERVICE_UNAVAILABLE, ex.getMessage());
			default -> new BusinessException(FileErrorCode.STORAGE_ERROR, ex.getMessage());
		};
	}

	private RuntimeException mapClientException(AmazonClientException ex) {
		Throwable cause = ex.getCause();
		if (cause instanceof SocketTimeoutException) {
			return new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_TIMEOUT, ex.getMessage());
		}
		if (cause instanceof ConnectException) {
			return new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, ex.getMessage());
		}
		return new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, ex.getMessage());
	}
}
