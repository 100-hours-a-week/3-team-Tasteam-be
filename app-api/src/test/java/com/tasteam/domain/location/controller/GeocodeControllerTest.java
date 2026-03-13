package com.tasteam.domain.location.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseControllerWebMvcTest;
import com.tasteam.domain.location.dto.response.ReverseGeocodeResponse;

@DisplayName("[유닛](Location) GeocodeController 단위 테스트")
class GeocodeControllerTest extends BaseControllerWebMvcTest {

	@Test
	@DisplayName("역지오코딩 요청에 대해 주소 결과를 반환한다")
	void 역지오코딩_성공() throws Exception {
		// given
		given(geocodeService.reverseGeocode(37.5, 127.0))
			.willReturn(new ReverseGeocodeResponse("서울특별시 강남구 역삼동", "강남구"));

		// when & then
		mockMvc.perform(get("/api/v1/geocode/reverse")
			.param("lat", "37.5")
			.param("lon", "127.0"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.address").value("서울특별시 강남구 역삼동"))
			.andExpect(jsonPath("$.data.district").value("강남구"));
	}

	@Test
	@DisplayName("위도 값 타입이 잘못되면 400으로 실패한다")
	void 역지오코딩_위도타입_오류_실패() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/geocode/reverse")
			.param("lat", "abc")
			.param("lon", "127.0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}
}
