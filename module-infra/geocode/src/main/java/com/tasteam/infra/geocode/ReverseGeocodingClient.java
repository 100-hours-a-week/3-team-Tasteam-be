package com.tasteam.infra.geocode;

import com.tasteam.infra.geocode.dto.ReverseGeocodingResult;

public interface ReverseGeocodingClient {

	ReverseGeocodingResult reverse(double lat, double lon);
}
