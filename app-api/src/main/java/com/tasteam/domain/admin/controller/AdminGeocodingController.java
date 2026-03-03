package com.tasteam.domain.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.controller.docs.AdminGeocodingControllerDocs;
import com.tasteam.domain.admin.dto.response.AdminGeocodingResponse;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/geocoding")
public class AdminGeocodingController implements AdminGeocodingControllerDocs {

	private final NaverGeocodingClient naverGeocodingClient;

	@Override
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminGeocodingResponse> geocode(
		@RequestParam
		String query) {

		GeocodingResult result = naverGeocodingClient.geocode(query);
		return SuccessResponse.success(new AdminGeocodingResponse(result.latitude(), result.longitude()));
	}
}
