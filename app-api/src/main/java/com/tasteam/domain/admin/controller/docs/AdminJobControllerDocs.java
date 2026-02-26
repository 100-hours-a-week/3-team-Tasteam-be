package com.tasteam.domain.admin.controller.docs;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.admin.dto.response.AdminCleanupPendingImageResponse;
import com.tasteam.domain.admin.dto.response.AdminJobResponse;
import com.tasteam.domain.admin.dto.response.AdminUnoptimizedImageResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(160)
@Tag(name = "Admin - Job", description = "어드민 배치 작업 API")
public interface AdminJobControllerDocs {

	@Operation(summary = "미최적화 이미지 목록 조회", description = "최적화되지 않은 이미지 목록을 조회합니다.")
	SuccessResponse<List<AdminUnoptimizedImageResponse>> getUnoptimizedImages(
		@Parameter(description = "조회 개수 제한", example = "100") @RequestParam
		int limit);

	@Operation(summary = "이미지 최적화 작업 실행", description = "이미지 최적화 배치 작업을 수동으로 실행합니다.")
	SuccessResponse<AdminJobResponse> runImageOptimization(
		@Parameter(description = "배치 처리 크기", example = "100") @RequestParam
		int batchSize);

	@Operation(summary = "삭제 대기 이미지 목록 조회", description = "삭제 대기 중인 이미지 목록을 조회합니다.")
	SuccessResponse<List<AdminCleanupPendingImageResponse>> getCleanupPendingImages();

	@Operation(summary = "이미지 정리 작업 실행", description = "삭제 대기 이미지를 정리하는 배치 작업을 수동으로 실행합니다.")
	SuccessResponse<AdminJobResponse> runImageCleanup();
}
