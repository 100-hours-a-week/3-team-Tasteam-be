package com.tasteam.infra.geocode;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.tasteam.infra.geocode.dto.NominatimReverseResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NominatimClient {

	private static final String BASE_URL = "https://nominatim.openstreetmap.org";

	private final RestClient restClient;

	public NominatimClient() {
		this.restClient = RestClient.builder()
			.baseUrl(BASE_URL)
			.defaultHeader(HttpHeaders.USER_AGENT, "Tasteam/1.0")
			.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ko")
			.build();
	}

	public NominatimReverseResponse reverse(double lat, double lon) {
		return restClient.get()
			.uri("/reverse?format=jsonv2&lat={lat}&lon={lon}&zoom=18&addressdetails=1", lat, lon)
			.retrieve()
			.body(NominatimReverseResponse.class);
	}
}
