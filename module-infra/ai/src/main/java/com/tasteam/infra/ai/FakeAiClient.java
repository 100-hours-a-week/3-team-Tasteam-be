package com.tasteam.infra.ai;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.client.RestClient;

import com.tasteam.infra.ai.dto.AiComparisonBatchRequest;
import com.tasteam.infra.ai.dto.AiComparisonBatchResponse;
import com.tasteam.infra.ai.dto.AiDebugInfo;
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
import com.tasteam.infra.ai.dto.AiVectorUploadRequest;
import com.tasteam.infra.ai.dto.AiVectorUploadResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FakeAiClient extends AiClient {

	private final AiClientProperties properties;

	public FakeAiClient(AiClientProperties properties) {
		super(RestClient.builder().baseUrl("http://localhost/fake-ai").build());
		this.properties = properties;
	}

	@Override
	public void healthCheck() {
		delay();
	}

	@Override
	public AiStrengthsResponse extractStrengths(AiStrengthsRequest request) {
		delay();
		return strengthsResponse(request.restaurantId());
	}

	@Override
	public AiComparisonBatchResponse extractStrengthsBatch(AiComparisonBatchRequest request) {
		delay();
		return new AiComparisonBatchResponse(List.of());
	}

	@Override
	public AiSummaryDisplayResponse summarize(AiSummaryRequest request) {
		delay();
		return summaryDisplay(request.restaurantId());
	}

	@Override
	public AiSummaryResponse summarizeDebug(AiSummaryRequest request) {
		delay();
		return new AiSummaryResponse(
			request.restaurantId(),
			"stub overall summary",
			Map.of("taste", categorySummary("stub taste summary")),
			List.of(),
			List.of(),
			3,
			1,
			debugInfo());
	}

	@Override
	public AiSummaryBatchResponse summarizeBatch(AiSummaryBatchRequest request) {
		delay();
		List<AiSummaryDisplayResponse> results = request.restaurants() == null
			? List.of()
			: request.restaurants().stream().map(item -> summaryDisplay(item.restaurantId())).toList();
		return new AiSummaryBatchResponse(results);
	}

	@Override
	public AiSentimentAnalysisDisplayResponse analyzeSentiment(AiSentimentRequest request) {
		delay();
		return new AiSentimentAnalysisDisplayResponse(request.restaurantId(), 70, 20);
	}

	@Override
	public AiSentimentAnalysisResponse analyzeSentimentDebug(AiSentimentRequest request) {
		delay();
		int totalCount = request.reviews() == null ? 0 : request.reviews().size();
		return new AiSentimentAnalysisResponse(
			request.restaurantId(),
			Math.max(1, (int)Math.round(totalCount * 0.7)),
			totalCount == 0 ? 0 : Math.max(0, (int)Math.round(totalCount * 0.2)),
			totalCount == 0 ? 0 : Math.max(0, totalCount - Math.max(1, (int)Math.round(totalCount * 0.7))
				- Math.max(0, (int)Math.round(totalCount * 0.2))),
			totalCount,
			70,
			20,
			10,
			debugInfo());
	}

	@Override
	public AiSentimentBatchResponse analyzeSentimentBatch(AiSentimentBatchRequest request) {
		delay();
		List<AiSentimentAnalysisResponse> results = request.restaurants() == null
			? List.of()
			: request.restaurants().stream()
				.map(item -> new AiSentimentAnalysisResponse(
					item.restaurantId(),
					7,
					2,
					1,
					10,
					70,
					20,
					10,
					debugInfo()))
				.toList();
		return new AiSentimentBatchResponse(results);
	}

	@Override
	public AiVectorUploadResponse uploadVectorData(AiVectorUploadRequest request) {
		delay();
		int pointsCount = request.reviews() == null ? 0 : request.reviews().size();
		return new AiVectorUploadResponse("stub vector upload ok", pointsCount, "tasteam-local-stub");
	}

	private void delay() {
		long millis = properties.getStubDelay() == null ? 100L : Math.max(0L, properties.getStubDelay().toMillis());
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("fake ai client sleep interrupted");
		}
	}

	private static AiStrengthsResponse strengthsResponse(long restaurantId) {
		return new AiStrengthsResponse(
			restaurantId,
			List.of(
				new AiStrengthsResponse.StrengthItem("taste", 12.5),
				new AiStrengthsResponse.StrengthItem("service", 8.0)),
			20,
			10,
			Map.of("taste", 12.5, "service", 8.0),
			List.of("맛이 안정적입니다.", "서비스 평가가 좋습니다."));
	}

	private static AiSummaryDisplayResponse summaryDisplay(long restaurantId) {
		return new AiSummaryDisplayResponse(
			restaurantId,
			"stub overall summary",
			Map.of(
				"taste", categorySummary("맛이 좋다는 평가가 많습니다."),
				"service", categorySummary("서비스가 친절하다는 평가가 있습니다.")));
	}

	private static AiSummaryDisplayResponse.CategorySummary categorySummary(String summary) {
		return new AiSummaryDisplayResponse.CategorySummary(
			summary,
			List.of(summary),
			List.of(new AiSummaryDisplayResponse.Evidence("1", "stub evidence", 1)));
	}

	private static AiDebugInfo debugInfo() {
		return new AiDebugInfo(
			UUID.randomUUID().toString(),
			100.0,
			0,
			"fake-ai-client",
			List.of("stub"));
	}
}
