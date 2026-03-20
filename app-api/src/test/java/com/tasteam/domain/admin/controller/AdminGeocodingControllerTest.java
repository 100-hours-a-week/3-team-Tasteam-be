package com.tasteam.domain.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseAdminControllerWebMvcTest;
import com.tasteam.infra.geocode.dto.GeocodingResult;

@DisplayName("[유닛](Admin) AdminGeocodingController 단위 테스트")
class AdminGeocodingControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("관리자 지오코딩")
	class Geocode {

		@Test
		@DisplayName("유효한 주소 문자열이면 위도/경도를 반환한다")
		void 지오코딩_성공() throws Exception {
			// given
			GeocodingResult result = new GeocodingResult("서울특별시", "강남구", "역삼동", "06200", 127.0, 37.5);
			given(naverGeocodingClient.geocode("서울시 강남구")).willReturn(result);

			// when & then
			mockMvc.perform(get("/api/v1/admin/geocoding").param("query", "서울시 강남구"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.latitude").value(37.5))
				.andExpect(jsonPath("$.data.longitude").value(127.0));
		}

		@Test
		@DisplayName("필수 쿼리 누락 시 내부 처리 오류로 실패한다")
		void 지오코딩_쿼리_누락_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/geocoding"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}
}
