package com.tasteam.infra.geocode;

import com.tasteam.infra.geocode.dto.GeocodingResult;

public interface GeocodingClient {

	GeocodingResult geocode(String query);
}
