package com.tasteam.domain.admin.controller.docs;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.admin.dto.response.AdminCleanupPendingImageResponse;
import com.tasteam.domain.admin.dto.response.AdminJobResponse;
import com.tasteam.domain.admin.dto.response.AdminPendingJobResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(160)
@Tag(name = "Admin - Job", description = "어드민 배치 작업 API")
public interface AdminJobControllerDocs {

	@Operation(summary = "이미지 최적화 대상 탐색", description = "S3 전체 이미지를 스캔하여 최적화 대상을 PENDING 잡으로 등록합니다.")
	@ApiResponse(responseCode = "200", description = "탐색 완료")
	SuccessResponse<AdminJobResponse> discoverOptimizationTargets();

	@Operation(summary = "PENDING 잡 목록 조회", description = "최적화 대기 중인 잡 목록을 조회합니다.")
	SuccessResponse<List<AdminPendingJobResponse>> getPendingJobs(
		@Parameter(description = "조회 개수 제한", example = "100") @RequestParam
		int limit);

	@Operation(summary = "이미지 최적화 작업 실행", description = "PENDING 잡을 배치로 처리합니다.")
	SuccessResponse<AdminJobResponse> runImageOptimization(
		@Parameter(description = "배치 처리 크기", example = "100") @RequestParam
		int batchSize);

	@Operation(summary = "최적화 잡 전체 삭제", description = "image_optimization_job 테이블을 전체 삭제합니다.")
	@ApiResponse(responseCode = "204", description = "삭제 완료")
	void deleteAllOptimizationJobs();

	@Operation(summary = "삭제 대기 이미지 목록 조회", description = "삭제 대기 중인 이미지 목록을 조회합니다.")
	SuccessResponse<List<AdminCleanupPendingImageResponse>> getCleanupPendingImages();

	@Operation(summary = "이미지 정리 작업 실행", description = "삭제 대기 이미지를 정리하는 배치 작업을 수동으로 실행합니다.")
	SuccessResponse<AdminJobResponse> runImageCleanup();
}
