package com.tasteam.domain.review.controller.docs;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;

import com.tasteam.domain.review.dto.response.ReviewDetailResponse;
import com.tasteam.domain.review.dto.response.ReviewKeywordItemResponse;
import com.tasteam.domain.review.entity.KeywordType;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Review", description = "리뷰 조회/삭제 API")
public interface ReviewControllerDocs {

	@Operation(summary = "리뷰 키워드 목록 조회", description = "리뷰에서 사용하는 키워드 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ReviewKeywordItemResponse.class)))
	SuccessResponse<List<ReviewKeywordItemResponse>> getReviewKeywords(
		@Parameter(description = "키워드 타입 필터", schema = @Schema(implementation = KeywordType.class))
		KeywordType type);

	@Operation(summary = "리뷰 상세 조회", description = "리뷰 ID 기준으로 상세 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ReviewDetailResponse.class)))
	SuccessResponse<ReviewDetailResponse> getReview(
		@Parameter(description = "리뷰 ID", example = "2001") @PathVariable
		Long reviewId);

	@Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다.")
	@ApiResponse(responseCode = "204", description = "삭제 완료")
	org.springframework.http.ResponseEntity<Void> deleteReview(
		@Parameter(description = "리뷰 ID", example = "2001") @PathVariable
		Long reviewId,
		@CurrentUser
		Long memberId);
}
