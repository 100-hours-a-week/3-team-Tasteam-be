package com.tasteam.domain.admin.controller.docs;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.admin.dto.request.AdminFoodCategoryCreateRequest;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(110)
@Tag(name = "Admin - Restaurant", description = "어드민 음식점 관리 API")
public interface AdminFoodCategoryControllerDocs {

	@Operation(summary = "음식 카테고리 등록", description = "새 음식 카테고리를 등록합니다.")
	SuccessResponse<Long> createFoodCategory(
		@Validated @RequestBody
		AdminFoodCategoryCreateRequest request);
}
