package com.tasteam.domain.admin.controller.docs;

import com.tasteam.domain.admin.dto.request.AdminRecommendationImportRequest;
import com.tasteam.domain.recommendation.importer.RecommendationResultImportResult;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@SwaggerTagOrder(165)
@Tag(name = "Admin - Recommendation Import", description = "개발/스테이징 추천 결과 수동 import API")
public interface AdminRecommendationImportControllerDocs {

	@Operation(summary = "추천 결과 수동 import 실행", description = "dev/stg 프로필에서만 추천 결과 S3 import를 수동 실행합니다.")
	@ApiResponse(responseCode = "200", description = "실행 완료")
	SuccessResponse<RecommendationResultImportResult> runRecommendationImport(@Valid
	AdminRecommendationImportRequest request);
}
