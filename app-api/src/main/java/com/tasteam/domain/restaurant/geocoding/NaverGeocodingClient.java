package com.tasteam.domain.restaurant.geocoding;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NaverGeocodingClient {

	private final RestClient naverMapsRestClient;

	public GeocodingResult geocode(String query) {

		NaverGeocodeResponse response;

		try {
			response = naverMapsRestClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/map-geocode/v2/geocode")
					.queryParam("query", query)
					.build())
				.retrieve()
				.body(NaverGeocodeResponse.class);

		} catch (RestClientResponseException e) {

			CommonErrorCode errorCode = e.getStatusCode().is4xxClientError()
				? CommonErrorCode.INVALID_REQUEST
				: CommonErrorCode.INTERNAL_SERVER_ERROR;

			throw new BusinessException(
				errorCode,
				e.getResponseBodyAsString());

		} catch (Exception e) {
			throw new BusinessException(
				CommonErrorCode.INTERNAL_SERVER_ERROR,
				"Geocoding API 호출 실패");
		}

		if (response == null) {
			throw new BusinessException(
				CommonErrorCode.INTERNAL_SERVER_ERROR,
				"Geocoding API 응답 없음");
		}

		NaverGeocodingErrorCode status = NaverGeocodingErrorCode.from(response.status());
		if (status != NaverGeocodingErrorCode.OK) {
			throw new BusinessException(
				status,
				response.errorMessage());
		}

		List<Address> addresses = response.addresses();
		if (addresses == null || addresses.isEmpty()) {
			throw new BusinessException(
				CommonErrorCode.INVALID_REQUEST,
				"Geocoding 결과가 없습니다");
		}

		return extract(addresses.getFirst());
	}

	private GeocodingResult extract(Address address) {

		String sido = null;
		String sigungu = null;
		String eupmyeondong = null;
		String postalCode = null;

		for (AddressElement element : address.addressElements()) {
			if (element.types().contains("SIDO")) {
				sido = element.longName();
			} else if (element.types().contains("SIGUGUN")) {
				sigungu = element.longName();
			} else if (element.types().contains("DONGMYUN")) {
				eupmyeondong = element.longName();
			} else if (element.types().contains("POSTAL_CODE")) {
				postalCode = element.longName();
			}
		}

		try {
			return new GeocodingResult(
				sido,
				sigungu,
				eupmyeondong,
				postalCode,
				Double.parseDouble(address.x()),
				Double.parseDouble(address.y()));

		} catch (NumberFormatException e) {
			throw new BusinessException(
				CommonErrorCode.INTERNAL_SERVER_ERROR,
				"좌표 파싱 실패");
		}
	}

	private record NaverGeocodeResponse(String status, List<Address> addresses, String errorMessage) {
	}
	private record Address(List<AddressElement> addressElements, String x, String y) {
	}
	private record AddressElement(List<String> types, String longName) {
	}
}
