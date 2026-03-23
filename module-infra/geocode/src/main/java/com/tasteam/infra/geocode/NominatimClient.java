package com.tasteam.infra.geocode;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.tasteam.infra.geocode.dto.NominatimReverseResponse;
import com.tasteam.infra.geocode.dto.ReverseGeocodingResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NominatimClient implements ReverseGeocodingClient {

	private static final String BASE_URL = "https://nominatim.openstreetmap.org";

	private final RestClient restClient;

	public NominatimClient() {
		this.restClient = RestClient.builder()
			.baseUrl(BASE_URL)
			.defaultHeader(HttpHeaders.USER_AGENT, "Tasteam/1.0")
			.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ko")
			.build();
	}

	@Override
	public ReverseGeocodingResult reverse(double lat, double lon) {
		NominatimReverseResponse response = restClient.get()
			.uri("/reverse?format=jsonv2&lat={lat}&lon={lon}&zoom=18&addressdetails=1", lat, lon)
			.retrieve()
			.body(NominatimReverseResponse.class);

		if (response == null) {
			return null;
		}

		NominatimReverseResponse.NominatimAddress address = response.address();
		return new ReverseGeocodingResult(
			response.displayName(),
			address != null ? address.city() : null,
			address != null ? address.county() : null,
			address != null ? address.borough() : null,
			address != null ? address.cityDistrict() : null,
			address != null ? address.suburb() : null,
			address != null ? address.neighbourhood() : null,
			address != null ? address.quarter() : null,
			address != null ? address.village() : null,
			address != null ? address.town() : null);
	}
}
