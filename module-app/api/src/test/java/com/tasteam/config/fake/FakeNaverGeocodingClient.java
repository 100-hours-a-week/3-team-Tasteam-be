package com.tasteam.config.fake;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.tasteam.infra.geocode.GeocodingClient;
import com.tasteam.infra.geocode.dto.GeocodingResult;

@Component
@Primary
@Profile("test")
public class FakeNaverGeocodingClient implements GeocodingClient {

	private static final GeocodingResult FIXED_RESULT = new GeocodingResult(
		"서울특별시", "강남구", "역삼동", "06234", 127.0365, 37.4979);

	@Override
	public GeocodingResult geocode(String query) {
		return FIXED_RESULT;
	}
}
