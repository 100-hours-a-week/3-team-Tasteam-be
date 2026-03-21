package com.tasteam.infra.geocode.dto;

public record GeocodingResult(
	String sido,
	String sigungu,
	String eupmyeondong,
	String postalCode,
	Double longitude,
	Double latitude) {
}
