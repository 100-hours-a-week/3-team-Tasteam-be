package com.tasteam.fixture;

import com.tasteam.domain.main.dto.request.MainPageRequest;

public final class MainPageRequestFixture {

	public static final Double DEFAULT_LATITUDE = 37.5665;
	public static final Double DEFAULT_LONGITUDE = 126.9780;

	private MainPageRequestFixture() {}

	public static MainPageRequest createRequest() {
		return new MainPageRequest(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
	}

	public static MainPageRequest createRequest(Double latitude, Double longitude) {
		return new MainPageRequest(latitude, longitude);
	}

	public static MainPageRequest createRequestWithoutLatitude() {
		return new MainPageRequest(null, DEFAULT_LONGITUDE);
	}

	public static MainPageRequest createRequestWithoutLongitude() {
		return new MainPageRequest(DEFAULT_LATITUDE, null);
	}

	public static MainPageRequest createRequestWithInvalidLatitude() {
		return new MainPageRequest(91.0, DEFAULT_LONGITUDE);
	}

	public static MainPageRequest createRequestWithInvalidLongitude() {
		return new MainPageRequest(DEFAULT_LATITUDE, 181.0);
	}
}
