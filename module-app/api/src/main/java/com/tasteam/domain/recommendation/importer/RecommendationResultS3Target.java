package com.tasteam.domain.recommendation.importer;

import java.time.LocalDate;

public record RecommendationResultS3Target(
	String resultFileS3Uri,
	String pipelineVersion,
	LocalDate batchDate) {

	public String dedupKey() {
		return pipelineVersion + "+" + batchDate;
	}
}
