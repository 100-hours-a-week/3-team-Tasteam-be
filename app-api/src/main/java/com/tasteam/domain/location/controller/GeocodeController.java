package com.tasteam.domain.location.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.location.controller.docs.GeocodeControllerDocs;
import com.tasteam.domain.location.dto.response.ReverseGeocodeResponse;
import com.tasteam.domain.location.service.GeocodeService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/geocode")
public class GeocodeController implements GeocodeControllerDocs {

	private final GeocodeService geocodeService;

	@Override
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/reverse")
	public SuccessResponse<ReverseGeocodeResponse> reverseGeocode(
		@RequestParam
		double lat,
		@RequestParam
		double lon) {
		return SuccessResponse.success(geocodeService.reverseGeocode(lat, lon));
	}
}
