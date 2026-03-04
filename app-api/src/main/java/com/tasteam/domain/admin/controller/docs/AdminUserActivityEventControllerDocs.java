package com.tasteam.domain.admin.controller.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.tasteam.domain.admin.dto.request.AdminUserActivityEventSearchCondition;
import com.tasteam.domain.admin.dto.response.AdminUserActivityEventListItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(150)
@Tag(name = "Admin - UserActivity", description = "어드민 사용자 활동 이벤트 조회 API")
public interface AdminUserActivityEventControllerDocs {

	@Operation(summary = "사용자 활동 이벤트 목록 조회", description = "user_activity_event 테이블의 이벤트를 필터링 및 페이지네이션으로 조회합니다.")
	SuccessResponse<Page<AdminUserActivityEventListItem>> getEvents(
		@Parameter(description = "검색 조건 (eventName, source, memberId, platform, occurredAtFrom, occurredAtTo 모두 선택)") @ModelAttribute
		AdminUserActivityEventSearchCondition condition,
		Pageable pageable);
}
