package com.tasteam.domain.admin.controller.docs;

import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.admin.dto.response.AdminGeocodingResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(110)
@Tag(name = "Admin - Restaurant", description = "어드민 음식점 관리 API")
public interface AdminGeocodingControllerDocs {

	@Operation(summary = "주소 좌표 변환", description = "주소 문자열을 위도/경도 좌표로 변환합니다.")
	SuccessResponse<AdminGeocodingResponse> geocode(
		@Parameter(description = "주소 검색어", example = "서울특별시 강남구 테헤란로 427") @RequestParam
		String query);
}
