package com.tasteam.domain.search.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseControllerWebMvcTest;
import com.tasteam.domain.search.dto.response.RecentSearchItem;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;

class RecentSearchControllerTest extends BaseControllerWebMvcTest {

	@Nested
	@DisplayName("최근 검색어 목록 조회")
	class GetRecentSearches {

		@Test
		@DisplayName("최근 검색어를 조회하면 검색어 목록을 반환한다")
		void 최근_검색어_조회_성공() throws Exception {
			// given
			OffsetPageResponse<RecentSearchItem> response = new OffsetPageResponse<>(
				List.of(
					new RecentSearchItem(1L, "맛집", Instant.now()),
					new RecentSearchItem(2L, "카페", Instant.now())),
				new OffsetPagination(0, 10, 1, 2));

			given(searchService.getRecentSearches(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/recent-searches"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].keyword").value("맛집"))
				.andExpect(jsonPath("$.data.items[1].keyword").value("카페"))
				.andExpect(jsonPath("$.data.pagination.totalElements").value(2));
		}
	}

	@Nested
	@DisplayName("최근 검색어 삭제")
	class DeleteRecentSearch {

		@Test
		@DisplayName("최근 검색어를 삭제하면 204를 반환한다")
		void 최근_검색어_삭제_성공() throws Exception {
			// given
			willDoNothing().given(searchService).deleteRecentSearch(any(), any());

			// when & then
			mockMvc.perform(delete("/api/v1/recent-searches/{id}", 1L))
				.andExpect(status().isNoContent());
		}
	}
}
