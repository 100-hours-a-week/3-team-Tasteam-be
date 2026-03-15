package com.tasteam.domain.main.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseControllerWebMvcTest;
import com.tasteam.fixture.MainPageRequestFixture;
import com.tasteam.fixture.MainPageResponseFixture;

@DisplayName("[유닛](Main) MainController 단위 테스트")
class MainControllerTest extends BaseControllerWebMvcTest {

	@Nested
	@DisplayName("메인 페이지 조회")
	class GetMain {

		@Test
		@DisplayName("위치 정보와 함께 메인 페이지를 조회하면 4개 섹션을 반환한다")
		void 위치_정보로_메인_페이지_조회_성공() throws Exception {
			given(mainService.getMain(any(), any())).willReturn(MainPageResponseFixture.createMainPageResponse());

			mockMvc.perform(get("/api/v1/main")
				.param("latitude", String.valueOf(MainPageRequestFixture.DEFAULT_LATITUDE))
				.param("longitude", String.valueOf(MainPageRequestFixture.DEFAULT_LONGITUDE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.banners.enabled").value(false))
				.andExpect(jsonPath("$.data.sections").isArray())
				.andExpect(jsonPath("$.data.sections.length()").value(4))
				.andExpect(jsonPath("$.data.sections[0].type").value("SPONSORED"))
				.andExpect(jsonPath("$.data.sections[0].items").isEmpty())
				.andExpect(jsonPath("$.data.sections[1].type").value("HOT"))
				.andExpect(jsonPath("$.data.sections[1].items[0].restaurantId").value(1))
				.andExpect(jsonPath("$.data.sections[2].type").value("NEW"))
				.andExpect(jsonPath("$.data.sections[3].type").value("AI_RECOMMEND"));
		}

		@Test
		@DisplayName("위도가 범위를 벗어나면 400 에러를 반환한다")
		void 위도_범위_초과시_400_에러() throws Exception {
			mockMvc.perform(get("/api/v1/main")
				.param("latitude", "91.0")
				.param("longitude", String.valueOf(MainPageRequestFixture.DEFAULT_LONGITUDE)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("경도가 범위를 벗어나면 400 에러를 반환한다")
		void 경도_범위_초과시_400_에러() throws Exception {
			mockMvc.perform(get("/api/v1/main")
				.param("latitude", String.valueOf(MainPageRequestFixture.DEFAULT_LATITUDE))
				.param("longitude", "181.0"))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("홈 페이지 조회")
	class GetHome {

		@Test
		@DisplayName("홈 페이지를 조회하면 배너, 스플래시와 함께 NEW/HOT 두 섹션을 반환한다")
		void 홈_페이지_조회_성공() throws Exception {
			given(mainService.getHome(any(), any())).willReturn(MainPageResponseFixture.createHomePageResponse());

			mockMvc.perform(get("/api/v1/main/home")
				.param("latitude", String.valueOf(MainPageRequestFixture.DEFAULT_LATITUDE))
				.param("longitude", String.valueOf(MainPageRequestFixture.DEFAULT_LONGITUDE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.banners.enabled").value(true))
				.andExpect(jsonPath("$.data.banners.items[0].id").value(10))
				.andExpect(jsonPath("$.data.splashPromotion.id").value(99))
				.andExpect(jsonPath("$.data.sections").isArray())
				.andExpect(jsonPath("$.data.sections.length()").value(2))
				.andExpect(jsonPath("$.data.sections[0].type").value("NEW"))
				.andExpect(jsonPath("$.data.sections[0].items[0].restaurantId").value(1))
				.andExpect(jsonPath("$.data.sections[1].type").value("HOT"));
		}
	}

	@Nested
	@DisplayName("AI 추천 페이지 조회")
	class GetAiRecommend {

		@Test
		@DisplayName("AI 추천 페이지를 조회하면 AI_RECOMMEND 단일 섹션을 반환한다")
		void AI_추천_조회_성공() throws Exception {
			given(mainService.getAiRecommend(any(), any())).willReturn(
				MainPageResponseFixture.createAiRecommendResponse());

			mockMvc.perform(get("/api/v1/main/ai-recommend")
				.param("latitude", String.valueOf(MainPageRequestFixture.DEFAULT_LATITUDE))
				.param("longitude", String.valueOf(MainPageRequestFixture.DEFAULT_LONGITUDE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.section.type").value("AI_RECOMMEND"))
				.andExpect(jsonPath("$.data.section.items[0].restaurantId").value(2));
		}
	}
}
