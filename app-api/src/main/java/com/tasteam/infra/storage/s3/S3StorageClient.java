package com.tasteam.infra.storage.s3;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tasteam.infra.storage.PresignedPostRequest;
import com.tasteam.infra.storage.PresignedPostResponse;
import com.tasteam.infra.storage.StorageClient;
import com.tasteam.infra.storage.StorageProperties;

import lombok.RequiredArgsConstructor;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class S3StorageClient implements StorageClient {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final DateTimeFormatter AMZ_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
		.withZone(ZoneOffset.UTC);

	private final AmazonS3 amazonS3;
	private final AWSCredentialsProvider credentialsProvider;
	private final StorageProperties properties;

	@Override
	public PresignedPostResponse createPresignedPost(PresignedPostRequest request) {
		Assert.notNull(request, "presigned 요청은 필수입니다");
		Assert.hasText(request.objectKey(), "objectKey는 필수입니다");
		Assert.hasText(request.contentType(), "contentType은 필수입니다");

		AWSCredentials credentials = credentialsProvider.getCredentials();
		Assert.hasText(credentials.getAWSAccessKeyId(), "AWS access key가 필요합니다");
		Assert.hasText(credentials.getAWSSecretKey(), "AWS secret key가 필요합니다");

		Instant now = Instant.now();
		Instant expiry = now.plusSeconds(properties.getPresignedExpirationSeconds());
		String amzDate = AMZ_DATE_FORMATTER.format(now);
		String dateStamp = amzDate.substring(0, 8);
		String region = resolveRegion();
		String credentialScope = dateStamp + "/" + region + "/s3/aws4_request";
		String credential = credentials.getAWSAccessKeyId() + "/" + credentialScope;

		String policyBase64 = createPolicy(request, expiry, credential, amzDate);
		byte[] signatureKey = getSignatureKey(credentials.getAWSSecretKey(), dateStamp, region, "s3");
		String signature = bytesToHex(hmacSha256(signatureKey, policyBase64));

		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("key", request.objectKey());
		fields.put("policy", policyBase64);
		fields.put("x-amz-algorithm", "AWS4-HMAC-SHA256");
		fields.put("x-amz-credential", credential);
		fields.put("x-amz-date", amzDate);
		fields.put("x-amz-signature", signature);
		fields.put("Content-Type", request.contentType());

		return new PresignedPostResponse(resolveBaseUrl(), Map.copyOf(fields), expiry);
	}

	@Override
	public void deleteObject(String objectKey) {
		Assert.hasText(objectKey, "objectKey는 필수입니다");
		amazonS3.deleteObject(new DeleteObjectRequest(resolveBucket(), objectKey));
	}

	private String createPolicy(PresignedPostRequest request, Instant expiry, String credential, String amzDate) {
		ObjectNode policyNode = OBJECT_MAPPER.createObjectNode();
		policyNode.put("expiration", expiry.toString());

		ArrayNode conditions = policyNode.putArray("conditions");
		conditions.add(OBJECT_MAPPER.createObjectNode().put("bucket", resolveBucket()));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("key", request.objectKey()));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("Content-Type", request.contentType()));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("x-amz-algorithm", "AWS4-HMAC-SHA256"));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("x-amz-credential", credential));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("x-amz-date", amzDate));

		try {
			byte[] policyBytes = OBJECT_MAPPER.writeValueAsBytes(policyNode);
			return Base64.getEncoder().encodeToString(policyBytes);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("policy 직렬화에 실패했습니다", ex);
		}
	}

	private byte[] hmacSha256(byte[] key, String data) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("signature 생성 실패", ex);
		}
	}

	private byte[] getSignatureKey(String secretKey, String dateStamp, String regionName, String serviceName) {
		byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
		byte[] kDate = hmacSha256(kSecret, dateStamp);
		byte[] kRegion = hmacSha256(kDate, regionName);
		byte[] kService = hmacSha256(kRegion, serviceName);
		return hmacSha256(kService, "aws4_request");
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
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
}
