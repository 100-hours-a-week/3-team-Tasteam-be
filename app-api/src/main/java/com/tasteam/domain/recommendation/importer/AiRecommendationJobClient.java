package com.tasteam.domain.recommendation.importer;

public interface AiRecommendationJobClient {

	AiRecommendationResponse requestRecommendation(AiRecommendationRequest request);
}
