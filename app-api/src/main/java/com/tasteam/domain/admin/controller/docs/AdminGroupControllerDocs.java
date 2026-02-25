package com.tasteam.domain.admin.controller.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.admin.dto.request.AdminGroupCreateRequest;
import com.tasteam.domain.admin.dto.response.AdminGroupListItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(120)
@Tag(name = "Admin - Group", description = "어드민 그룹 관리 API")
public interface AdminGroupControllerDocs {

	@Operation(summary = "그룹 목록 조회", description = "전체 그룹 목록을 최신순으로 조회합니다.")
	SuccessResponse<Page<AdminGroupListItem>> getGroups(Pageable pageable);

	@Operation(summary = "그룹 생성", description = "어드민이 새 그룹을 직접 생성합니다.")
	SuccessResponse<Long> createGroup(
		@Validated @RequestBody
		AdminGroupCreateRequest request);
}
