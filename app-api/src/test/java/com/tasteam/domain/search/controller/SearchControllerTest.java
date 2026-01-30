package com.tasteam.domain.search.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.search.dto.response.SearchGroupSummary;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.domain.search.dto.response.SearchRestaurantItem;
import com.tasteam.domain.search.service.SearchService;
import com.tasteam.fixture.SearchRequestFixture;

@ControllerWebMvcTest(SearchController.class)
class SearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SearchService searchService;

	@Nested
	@DisplayName("통합 검색")
	class Search {

		@Test
		@DisplayName("키워드로 검색하면 그룹과 레스토랑 목록을 반환한다")
		void 키워드_검색_성공() throws Exception {
			// given
			SearchResponse response = new SearchResponse(
				List.of(
					new SearchGroupSummary(
						1L,
						"맛집모임",
						new SearchGroupSummary.LogoImage(
							java.util.UUID.fromString("a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012"),
							"https://example.com/logo.jpg"),
						10L)),
				new CursorPageResponse<>(
					List.of(
						new SearchRestaurantItem(1L, "맛집식당", "서울시 강남구", "https://example.com/img.jpg")),
					new CursorPageResponse.Pagination(null, false, 20)));

			given(searchService.search(any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/search")
				.param("keyword", SearchRequestFixture.DEFAULT_KEYWORD)
				.param("size", String.valueOf(SearchRequestFixture.DEFAULT_SIZE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.groups").isArray())
				.andExpect(jsonPath("$.data.groups[0].groupId").value(1))
				.andExpect(jsonPath("$.data.groups[0].name").value("맛집모임"))
				.andExpect(jsonPath("$.data.restaurants.items").isArray())
				.andExpect(jsonPath("$.data.restaurants.items[0].restaurantId").value(1))
				.andExpect(jsonPath("$.data.restaurants.items[0].name").value("맛집식당"))
				.andExpect(jsonPath("$.data.restaurants.pagination.hasNext").value(false));
		}

		@Test
		@DisplayName("커서와 함께 검색하면 다음 페이지 결과를 반환한다")
		void 커서_기반_페이징_검색_성공() throws Exception {
			// given
			SearchResponse response = new SearchResponse(
				List.of(),
				new CursorPageResponse<>(
					List.of(
						new SearchRestaurantItem(2L, "다음맛집", "서울시 서초구", "https://example.com/img2.jpg")),
					new CursorPageResponse.Pagination("next-cursor", true, 20)));

			given(searchService.search(any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/search")
				.param("keyword", SearchRequestFixture.DEFAULT_KEYWORD)
				.param("cursor", "some-cursor")
				.param("size", String.valueOf(SearchRequestFixture.DEFAULT_SIZE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.restaurants.pagination.hasNext").value(true))
				.andExpect(jsonPath("$.data.restaurants.pagination.nextCursor").value("next-cursor"));
		}

		@Test
		@DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
		void 검색_결과_없음() throws Exception {
			// given
			given(searchService.search(any(), any())).willReturn(SearchResponse.emptyResponse());

			// when & then
			mockMvc.perform(post("/api/v1/search")
				.param("keyword", "없는키워드")
				.param("size", String.valueOf(SearchRequestFixture.DEFAULT_SIZE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.groups").isEmpty())
				.andExpect(jsonPath("$.data.restaurants.items").isEmpty());
		}

		@Test
		@DisplayName("키워드가 없으면 400 에러를 반환한다")
		void 키워드_누락시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/search")
				.param("size", String.valueOf(SearchRequestFixture.DEFAULT_SIZE)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("키워드가 빈 문자열이면 400 에러를 반환한다")
		void 키워드_빈문자열시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/search")
				.param("keyword", "")
				.param("size", String.valueOf(SearchRequestFixture.DEFAULT_SIZE)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("size가 100을 초과하면 400 에러를 반환한다")
		void size_범위_초과시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/search")
				.param("keyword", SearchRequestFixture.DEFAULT_KEYWORD)
				.param("size", "101"))
				.andExpect(status().isBadRequest());
		}
	}
}
