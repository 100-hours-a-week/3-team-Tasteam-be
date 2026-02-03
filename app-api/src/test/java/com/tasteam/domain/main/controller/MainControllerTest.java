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
import com.tasteam.domain.main.dto.response.AiRecommendResponse;
import com.tasteam.domain.main.dto.response.HomePageResponse;
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

	private MainPageResponse createMockResponse() {
		SectionItem item = new SectionItem(1L, "맛집1", 100.0, "한식", "https://example.com/img1.jpg", false, "맛있어요");
		return new MainPageResponse(
			new Banners(false, List.of()),
			List.of(
				new Section("SPONSORED", "Sponsored", List.of()),
				new Section("HOT", "이번주 Hot", List.of(item)),
				new Section("NEW", "신규 개장", List.of(item)),
				new Section("AI_RECOMMEND", "AI 추천", List.of(item))));
	}

	private HomePageResponse createHomeResponse() {
		HomePageResponse.SectionItem item = new HomePageResponse.SectionItem(
			1L, "맛집1", 120.0, "한식", "https://example.com/img1.jpg", "요약");
		return new HomePageResponse(
			List.of(
				new HomePageResponse.Section("NEW", "신규 개장", List.of(item)),
				new HomePageResponse.Section("HOT", "이번주 Hot", List.of(item))));
	}

	private AiRecommendResponse createAiResponse() {
		AiRecommendResponse.SectionItem item = new AiRecommendResponse.SectionItem(
			2L, "카페", 80.0, "카페", "https://example.com/img2.jpg", "AI 요약");
		return new AiRecommendResponse(
			new AiRecommendResponse.Section("AI_RECOMMEND", "AI 추천", List.of(item)));
	}

	@Nested
	@DisplayName("메인 페이지 조회")
	class GetMain {

		@Test
		@DisplayName("위치 정보와 함께 메인 페이지를 조회하면 4개 섹션을 반환한다")
		void 위치_정보로_메인_페이지_조회_성공() throws Exception {
			// given
			given(mainService.getMain(any(), any())).willReturn(createMockResponse());

			// when & then
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

	@Nested
	@DisplayName("홈 페이지 조회")
	class GetHome {

		@Test
		@DisplayName("홈 페이지를 조회하면 NEW/HOT 두 섹션만 반환한다")
		void 홈_페이지_조회_성공() throws Exception {
			// given
			given(mainService.getHome(any(), any())).willReturn(createHomeResponse());

			// when & then
			mockMvc.perform(get("/api/v1/main/home")
				.param("latitude", String.valueOf(MainPageRequestFixture.DEFAULT_LATITUDE))
				.param("longitude", String.valueOf(MainPageRequestFixture.DEFAULT_LONGITUDE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
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
			// given
			given(mainService.getAiRecommend(any(), any())).willReturn(createAiResponse());

			// when & then
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
