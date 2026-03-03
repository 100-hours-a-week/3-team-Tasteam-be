package com.tasteam.infra.geocode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NominatimReverseResponse(
	@JsonProperty("display_name")
	String displayName,
	NominatimAddress address) {

	public record NominatimAddress(
		String city,
		String county,
		String borough,
		@JsonProperty("city_district")
		String cityDistrict,
		String suburb,
		String neighbourhood,
		String quarter,
		String village,
		String town) {
	}
}
