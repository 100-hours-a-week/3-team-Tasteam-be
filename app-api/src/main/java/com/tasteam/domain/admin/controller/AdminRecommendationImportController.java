package com.tasteam.domain.admin.controller;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.batch.recommendation.runner.RecommendationImportBatchRunner;
import com.tasteam.domain.admin.controller.docs.AdminRecommendationImportControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminRecommendationImportRequest;
import com.tasteam.domain.recommendation.importer.RecommendationResultImportResult;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Profile({"dev", "stg"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/recommendations")
public class AdminRecommendationImportController implements AdminRecommendationImportControllerDocs {

	private final RecommendationImportBatchRunner recommendationImportBatchRunner;

	@Override
	@PostMapping("/import")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<RecommendationResultImportResult> runRecommendationImport(
		@Valid @RequestBody
		AdminRecommendationImportRequest request) {
		String requestId = request.requestId() == null || request.requestId().isBlank()
			? "admin-import-" + UUID.randomUUID()
			: request.requestId();
		RecommendationResultImportResult result = recommendationImportBatchRunner.runOnDemand(
			request.modelVersion(),
			request.s3PrefixOrUri(),
			requestId);
		return SuccessResponse.success(result);
	}
}
