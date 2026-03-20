package com.tasteam.config.fake;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.tasteam.infra.geocode.dto.GeocodingResult;
import com.tasteam.infra.geocode.naver.NaverGeocodingClient;

@Component
@Primary
@Profile("test")
public class FakeNaverGeocodingClient extends NaverGeocodingClient {

	private static final GeocodingResult FIXED_RESULT = new GeocodingResult(
		"서울특별시", "강남구", "역삼동", "06234", 127.0365, 37.4979);

	public FakeNaverGeocodingClient() {
		super(null);
	}

	@Override
	public GeocodingResult geocode(String query) {
		return FIXED_RESULT;
	}
}
