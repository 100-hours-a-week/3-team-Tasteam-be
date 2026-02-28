package com.tasteam.domain.admin.controller.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.admin.dto.response.AdminReviewListItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(135)
@Tag(name = "Admin - Review", description = "어드민 리뷰 관리 API")
public interface AdminReviewControllerDocs {

	@Operation(summary = "리뷰 목록 조회", description = "전체 리뷰를 최신순으로 조회합니다. restaurantId로 특정 음식점 리뷰만 필터링할 수 있습니다.")
	SuccessResponse<Page<AdminReviewListItem>> getReviews(
		@Parameter(description = "음식점 ID (선택)") @RequestParam(required = false)
		Long restaurantId,
		Pageable pageable);

	@Operation(summary = "리뷰 삭제", description = "리뷰를 소프트 삭제합니다.")
	void deleteReview(
		@Parameter(description = "리뷰 ID", example = "1") @PathVariable
		Long reviewId);
}
