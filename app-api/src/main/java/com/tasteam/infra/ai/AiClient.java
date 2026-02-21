package com.tasteam.infra.ai;

import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.tasteam.infra.ai.dto.AiSentimentAnalysisDisplayResponse;
import com.tasteam.infra.ai.dto.AiSentimentAnalysisResponse;
import com.tasteam.infra.ai.dto.AiSentimentBatchRequest;
import com.tasteam.infra.ai.dto.AiSentimentBatchResponse;
import com.tasteam.infra.ai.dto.AiSentimentRequest;
import com.tasteam.infra.ai.dto.AiStrengthsRequest;
import com.tasteam.infra.ai.dto.AiStrengthsResponse;
import com.tasteam.infra.ai.dto.AiSummaryBatchRequest;
import com.tasteam.infra.ai.dto.AiSummaryBatchResponse;
import com.tasteam.infra.ai.dto.AiSummaryDisplayResponse;
import com.tasteam.infra.ai.dto.AiSummaryRequest;
import com.tasteam.infra.ai.dto.AiSummaryResponse;
import com.tasteam.infra.ai.dto.AiVectorSearchRequest;
import com.tasteam.infra.ai.dto.AiVectorSearchResponse;
import com.tasteam.infra.ai.dto.AiVectorUploadRequest;
import com.tasteam.infra.ai.dto.AiVectorUploadResponse;
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
		execute("health check", requestId -> {
			aiRestClient.get()
				.uri("/health")
				.header(REQUEST_ID_HEADER, requestId)
				.retrieve()
				.toBodilessEntity();
			return null;
		});
	}

	public AiStrengthsResponse extractStrengths(AiStrengthsRequest request) {
		return execute("extract strengths", requestId -> aiRestClient.post()
			.uri("/api/v1/llm/extract/strengths")
			.header(REQUEST_ID_HEADER, requestId)
			.body(request)
			.retrieve()
			.body(AiStrengthsResponse.class));
	}

	public AiSummaryDisplayResponse summarize(AiSummaryRequest request) {
		return execute("summarize", requestId -> aiRestClient.post()
			.uri("/api/v1/llm/summarize")
			.header(REQUEST_ID_HEADER, requestId)
			.body(request)
			.retrieve()
			.body(AiSummaryDisplayResponse.class));
	}

	public AiSummaryResponse summarizeDebug(AiSummaryRequest request) {
		return execute("summarize debug", requestId -> aiRestClient.post()
			.uri("/api/v1/llm/summarize")
			.header(REQUEST_ID_HEADER, requestId)
			.header("X-Debug", "true")
			.body(request)
			.retrieve()
			.body(AiSummaryResponse.class));
	}

	public AiSummaryBatchResponse summarizeBatch(AiSummaryBatchRequest request) {
		return execute("summarize batch", requestId -> aiRestClient.post()
			.uri("/api/v1/llm/summarize/batch")
			.header(REQUEST_ID_HEADER, requestId)
			.body(request)
			.retrieve()
			.body(AiSummaryBatchResponse.class));
	}

	public AiSentimentAnalysisDisplayResponse analyzeSentiment(AiSentimentRequest request) {
		return execute("sentiment analyze", requestId -> aiRestClient.post()
			.uri("/api/v1/sentiment/analyze")
			.header(REQUEST_ID_HEADER, requestId)
			.body(request)
			.retrieve()
			.body(AiSentimentAnalysisDisplayResponse.class));
	}

	public AiSentimentAnalysisResponse analyzeSentimentDebug(AiSentimentRequest request) {
		return execute("sentiment analyze debug", requestId -> aiRestClient.post()
			.uri("/api/v1/sentiment/analyze")
			.header(REQUEST_ID_HEADER, requestId)
			.header("X-Debug", "true")
			.body(request)
			.retrieve()
			.body(AiSentimentAnalysisResponse.class));
	}

	public AiSentimentBatchResponse analyzeSentimentBatch(AiSentimentBatchRequest request) {
		return execute("sentiment analyze batch", requestId -> aiRestClient.post()
			.uri("/api/v1/sentiment/analyze/batch")
			.header(REQUEST_ID_HEADER, requestId)
			.body(request)
			.retrieve()
			.body(AiSentimentBatchResponse.class));
	}

	public AiVectorSearchResponse searchSimilarReviews(AiVectorSearchRequest request) {
		return execute("vector search similar", requestId -> aiRestClient.post()
			.uri("/api/v1/vector/search/similar")
			.header(REQUEST_ID_HEADER, requestId)
			.body(request)
			.retrieve()
			.body(AiVectorSearchResponse.class));
	}

	public AiVectorUploadResponse uploadVectorData(AiVectorUploadRequest request) {
		return execute("vector upload", requestId -> aiRestClient.post()
			.uri("/api/v1/vector/upload")
			.header(REQUEST_ID_HEADER, requestId)
			.body(request)
			.retrieve()
			.body(AiVectorUploadResponse.class));
	}

	private <T> T execute(String action, Function<String, T> call) {
		String requestId = UUID.randomUUID().toString();
		try {
			return call.apply(requestId);

		} catch (Exception ex) {
			throw switch (ex) {
				case RestClientResponseException r -> {
					log.error("AI {} failed. requestId={}, status={}, body={}",
						action, requestId, r.getStatusCode().value(), r.getResponseBodyAsString());
					yield AiServerException.from(r, requestId);
				}
				case ResourceAccessException r -> {
					log.error("AI {} timeout. requestId={}, message={}", action, requestId, r.getMessage());
					yield AiServerException.timeout(requestId);
				}
				default -> {
					log.error("AI {} error. requestId={}, message={}", action, requestId, ex.getMessage(), ex);
					yield AiServerException.unknown(ex, requestId);
				}
			};
		}
	}
}
