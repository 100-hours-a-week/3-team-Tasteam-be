package com.tasteam.infra.ai;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.tasteam.infra.ai.exception.AiErrorCode;
import com.tasteam.infra.ai.exception.AiServerException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	private final RestClient aiRestClient;

	public void healthCheck() {
		String requestId = UUID.randomUUID().toString();
		try {
			aiRestClient.post()
				.uri("/health")
				.header(REQUEST_ID_HEADER, requestId)
				.retrieve()
				.toBodilessEntity();

		} catch (RestClientResponseException e) {
			log.error("AI health check failed. requestId={}, status={}, body={}",
				requestId, e.getStatusCode().value(), e.getResponseBodyAsString());
			throw new AiServerException(AiErrorCode.AI_SERVER_ERROR, e.getResponseBodyAsString());

		} catch (ResourceAccessException e) {
			log.error("AI health check timeout. requestId={}, message={}", requestId, e.getMessage());
			throw new AiServerException(AiErrorCode.AI_TIMEOUT, e.getMessage());

		} catch (Exception e) {
			log.error("AI health check error. requestId={}, message={}", requestId, e.getMessage(), e);
			throw new AiServerException(AiErrorCode.AI_UNAVAILABLE, e.getMessage());
		}
	}
}
