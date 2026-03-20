package com.tasteam.infra.geocode.dto;

public record ReverseGeocodingResult(
	String displayName,
	String city,
	String county,
	String borough,
	String cityDistrict,
	String suburb,
	String neighbourhood,
	String quarter,
	String village,
	String town) {
}
