package com.tasteam.infra.storage.s3.policy;

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

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tasteam.infra.storage.PresignedPostRequest;

/**
 * Builds SigV4 policy and signature for S3 POST uploads.
 * Keeps cryptographic details isolated from the storage client.
 */
@Component
public class S3PresignPolicyBuilder {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final DateTimeFormatter AMZ_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
		.withZone(ZoneOffset.UTC);
	private static final String AWS4_PREFIX = "AWS4";
	private static final String AWS4_ALGORITHM = "AWS4-HMAC-SHA256";
	private static final String AWS4_REQUEST = "aws4_request";
	private static final String S3_SERVICE_NAME = "s3";
	private static final int DATE_STAMP_LENGTH = 8;

	public S3PresignPolicy build(PresignedPostRequest request, AWSCredentials credentials, String region, String bucket,
		long expirationSeconds) {
		Assert.notNull(request, "presigned 요청은 필수입니다");
		Assert.notNull(credentials, "AWS 자격증명이 필요합니다");
		Assert.hasText(region, "region은 필수입니다");
		Assert.hasText(bucket, "bucket은 필수입니다");

		String accessKey = credentials.getAWSAccessKeyId();
		String secretKey = credentials.getAWSSecretKey();
		Assert.hasText(accessKey, "AWS access key가 필요합니다");
		Assert.hasText(secretKey, "AWS secret key가 필요합니다");

		String sessionToken = null;
		if (credentials instanceof AWSSessionCredentials sessionCredentials) {
			sessionToken = sessionCredentials.getSessionToken();
		}

		Instant now = Instant.now();
		Instant expiry = now.plusSeconds(expirationSeconds);
		String amzDate = AMZ_DATE_FORMATTER.format(now);
		String dateStamp = amzDate.substring(0, DATE_STAMP_LENGTH);
		String credentialScope = dateStamp + "/" + region + "/" + S3_SERVICE_NAME + "/" + AWS4_REQUEST;
		String credential = accessKey + "/" + credentialScope;

		String policyBase64 = createPolicy(request, bucket, expiry, credential, amzDate, sessionToken);
		byte[] signatureKey = getSignatureKey(secretKey, dateStamp, region, S3_SERVICE_NAME);
		String signature = bytesToHex(hmacSha256(signatureKey, policyBase64));

		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("key", request.objectKey());
		fields.put("policy", policyBase64);
		fields.put("x-amz-algorithm", AWS4_ALGORITHM);
		fields.put("x-amz-credential", credential);
		fields.put("x-amz-date", amzDate);
		fields.put("x-amz-signature", signature);
		if (sessionToken != null) {
			fields.put("x-amz-security-token", sessionToken);
		}
		fields.put("Content-Type", request.contentType());

		return S3PresignPolicy.builder()
			.fields(Map.copyOf(fields))
			.expiresAt(expiry)
			.build();
	}

	private String createPolicy(PresignedPostRequest request, String bucket, Instant expiry, String credential,
		String amzDate, String sessionToken) {
		ObjectNode policyNode = OBJECT_MAPPER.createObjectNode();
		policyNode.put("expiration", expiry.toString());

		ArrayNode conditions = policyNode.putArray("conditions");
		conditions.add(OBJECT_MAPPER.createObjectNode().put("bucket", bucket));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("key", request.objectKey()));
		ArrayNode contentLengthRange = OBJECT_MAPPER.createArrayNode()
			.add("content-length-range")
			.add(request.minContentLength())
			.add(request.maxContentLength());
		conditions.add(contentLengthRange);
		conditions.add(OBJECT_MAPPER.createObjectNode().put("Content-Type", request.contentType()));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("x-amz-algorithm", AWS4_ALGORITHM));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("x-amz-credential", credential));
		conditions.add(OBJECT_MAPPER.createObjectNode().put("x-amz-date", amzDate));
		if (sessionToken != null) {
			conditions.add(OBJECT_MAPPER.createObjectNode().put("x-amz-security-token", sessionToken));
		}

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
		byte[] kSecret = (AWS4_PREFIX + secretKey).getBytes(StandardCharsets.UTF_8);
		byte[] kDate = hmacSha256(kSecret, dateStamp);
		byte[] kRegion = hmacSha256(kDate, regionName);
		byte[] kService = hmacSha256(kRegion, serviceName);
		return hmacSha256(kService, AWS4_REQUEST);
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}
}
