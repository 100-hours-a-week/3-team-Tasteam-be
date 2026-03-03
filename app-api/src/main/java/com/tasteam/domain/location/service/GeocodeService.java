package com.tasteam.domain.location.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.tasteam.domain.location.dto.response.ReverseGeocodeResponse;
import com.tasteam.infra.geocode.NominatimClient;
import com.tasteam.infra.geocode.dto.NominatimReverseResponse;
import com.tasteam.infra.geocode.dto.NominatimReverseResponse.NominatimAddress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodeService {

	private static final String DEFAULT_LOCATION = "현재 위치";

	private final NominatimClient nominatimClient;

	public ReverseGeocodeResponse reverseGeocode(double lat, double lon) {
		try {
			NominatimReverseResponse response = nominatimClient.reverse(lat, lon);
			String address = response.displayName() != null ? response.displayName() : DEFAULT_LOCATION;
			String district = pickDistrict(response.address());
			return new ReverseGeocodeResponse(address, district);
		} catch (RestClientException e) {
			log.warn("Nominatim reverse geocode 실패: lat={}, lon={}, message={}", lat, lon, e.getMessage());
			return new ReverseGeocodeResponse(DEFAULT_LOCATION, DEFAULT_LOCATION);
		}
	}

	private String pickDistrict(NominatimAddress address) {
		if (address == null) {
			return DEFAULT_LOCATION;
		}
		String gu = firstNonBlank(address.borough(), address.cityDistrict(), address.county(), address.city(),
			address.town());
		String dong = firstNonBlank(address.suburb(), address.neighbourhood(), address.quarter(), address.village());
		String result = (gu + " " + dong).trim();
		return result.isBlank() ? DEFAULT_LOCATION : result;
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
