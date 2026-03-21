package com.tasteam.domain.location.controller.docs;

import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.location.dto.response.ReverseGeocodeResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(33)
@Tag(name = "Location", description = "위치 정보 API")
public interface GeocodeControllerDocs {

	@Operation(summary = "역지오코딩", description = "위도/경도 좌표를 주소 문자열로 변환합니다.")
	SuccessResponse<ReverseGeocodeResponse> reverseGeocode(
		@Parameter(description = "위도", example = "37.5665") @RequestParam
		double lat,
		@Parameter(description = "경도", example = "126.9780") @RequestParam
		double lon);
}
