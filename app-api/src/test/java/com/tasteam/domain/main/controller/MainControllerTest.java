package com.tasteam.domain.main.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse.Banners;
import com.tasteam.domain.main.dto.response.MainPageResponse.Section;
import com.tasteam.domain.main.dto.response.MainPageResponse.SectionItem;
import com.tasteam.domain.main.service.MainService;
import com.tasteam.fixture.MainPageRequestFixture;

@ControllerWebMvcTest(MainController.class)
class MainControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MainService mainService;

	@Nested
	@DisplayName("메인 페이지 조회")
	class GetMain {

		@Test
		@DisplayName("위치 정보와 함께 메인 페이지를 조회하면 섹션 목록을 반환한다")
		void 위치_정보로_메인_페이지_조회_성공() throws Exception {
			// given
			MainPageResponse response = new MainPageResponse(
				new Banners(false, List.of()),
				List.of(
					new Section("SPONSORED", "Sponsored", List.of(
						new SectionItem(1L, "맛집1", 100.0, "한식", "https://example.com/img1.jpg", false, "맛있어요"))),
					new Section("HOT", "이번주 Hot", List.of())));

			given(mainService.getMain(any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/main")
				.param("latitude", String.valueOf(MainPageRequestFixture.DEFAULT_LATITUDE))
				.param("longitude", String.valueOf(MainPageRequestFixture.DEFAULT_LONGITUDE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.banners.enabled").value(false))
				.andExpect(jsonPath("$.data.sections").isArray())
				.andExpect(jsonPath("$.data.sections[0].type").value("SPONSORED"))
				.andExpect(jsonPath("$.data.sections[0].title").value("Sponsored"))
				.andExpect(jsonPath("$.data.sections[0].items[0].restaurantId").value(1))
				.andExpect(jsonPath("$.data.sections[0].items[0].name").value("맛집1"));
		}

		@Test
		@DisplayName("위도가 없으면 400 에러를 반환한다")
		void 위도_누락시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/main")
				.param("longitude", String.valueOf(MainPageRequestFixture.DEFAULT_LONGITUDE)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("경도가 없으면 400 에러를 반환한다")
		void 경도_누락시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/main")
				.param("latitude", String.valueOf(MainPageRequestFixture.DEFAULT_LATITUDE)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("위도가 범위를 벗어나면 400 에러를 반환한다")
		void 위도_범위_초과시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/main")
				.param("latitude", "91.0")
				.param("longitude", String.valueOf(MainPageRequestFixture.DEFAULT_LONGITUDE)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("경도가 범위를 벗어나면 400 에러를 반환한다")
		void 경도_범위_초과시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/main")
				.param("latitude", String.valueOf(MainPageRequestFixture.DEFAULT_LATITUDE))
				.param("longitude", "181.0"))
				.andExpect(status().isBadRequest());
		}
	}
}
