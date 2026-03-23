package com.tasteam.domain.recommendation.importer;

public interface RecommendationResultImportFacade {

	RecommendationResultImportResult importResults(RecommendationResultImportFacadeCommand command);
}
