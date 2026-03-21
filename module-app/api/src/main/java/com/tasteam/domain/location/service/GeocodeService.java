package com.tasteam.domain.location.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.tasteam.domain.location.dto.response.ReverseGeocodeResponse;
import com.tasteam.infra.geocode.ReverseGeocodingClient;
import com.tasteam.infra.geocode.dto.ReverseGeocodingResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodeService {

	private static final String DEFAULT_LOCATION = "현재 위치";

	private final ReverseGeocodingClient reverseGeocodingClient;

	@Cacheable(cacheNames = "reverse-geocode", key = "T(String).format('%.3f_%.3f', #lat, #lon)")
	public ReverseGeocodeResponse reverseGeocode(double lat, double lon) {
		try {
			ReverseGeocodingResult result = reverseGeocodingClient.reverse(lat, lon);
			if (result == null) {
				return new ReverseGeocodeResponse(DEFAULT_LOCATION, DEFAULT_LOCATION);
			}
			String address = result.displayName() != null ? result.displayName() : DEFAULT_LOCATION;
			String district = pickDistrict(result);
			return new ReverseGeocodeResponse(address, district);
		} catch (RestClientException e) {
			log.warn("ReverseGeocode 실패: lat={}, lon={}, message={}", lat, lon, e.getMessage());
			return new ReverseGeocodeResponse(DEFAULT_LOCATION, DEFAULT_LOCATION);
		}
	}

	private String pickDistrict(ReverseGeocodingResult result) {
		String gu = firstNonBlank(result.borough(), result.cityDistrict(), result.county(), result.city(),
			result.town());
		String dong = firstNonBlank(result.suburb(), result.neighbourhood(), result.quarter(), result.village());
		String district = (gu + " " + dong).trim();
		return district.isBlank() ? DEFAULT_LOCATION : district;
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return "";
	}
}
