package com.tasteam.domain.promotion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.promotion.dto.response.PromotionDetailResponse;
import com.tasteam.domain.promotion.dto.response.PromotionSummaryResponse;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;

@ControllerWebMvcTest(PromotionController.class)
class PromotionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PromotionService promotionService;

	private OffsetPageResponse<PromotionSummaryResponse> createMockListResponse() {
		PromotionSummaryResponse item = new PromotionSummaryResponse(
			1L,
			"신년 특가",
			"새해 맞이 할인",
			"https://example.com/promotion",
			"https://example.com/banner.jpg",
			Instant.parse("2026-01-01T00:00:00Z"),
			Instant.parse("2026-01-31T23:59:59Z"),
			PromotionStatus.ONGOING,
			Instant.parse("2026-01-01T00:00:00Z"),
			Instant.parse("2026-01-31T23:59:59Z"));

		return new OffsetPageResponse<>(
			List.of(item),
			new OffsetPagination(0, 10, 1, 1));
	}

	private PromotionDetailResponse createMockDetailResponse() {
		return new PromotionDetailResponse(
			1L,
			"신년 특가",
			"새해 맞이 할인 행사입니다.",
			"https://example.com/promotion",
			Instant.parse("2026-01-01T00:00:00Z"),
			Instant.parse("2026-01-31T23:59:59Z"),
			PromotionStatus.ONGOING,
			Instant.parse("2026-01-01T00:00:00Z"),
			Instant.parse("2026-01-31T23:59:59Z"),
			"https://example.com/banner.jpg",
			List.of("https://example.com/detail1.jpg", "https://example.com/detail2.jpg"));
	}

	@Nested
	@DisplayName("프로모션 목록 조회")
	class GetPromotionList {

		@Test
		@DisplayName("프로모션 목록을 조회하면 페이징된 결과를 반환한다")
		void getPromotionList_returnsPagedResult() throws Exception {
			given(promotionService.getPromotionList(any(), any())).willReturn(createMockListResponse());

			mockMvc.perform(get("/api/v1/promotions")
				.param("page", "0")
				.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].title").value("신년 특가"))
				.andExpect(jsonPath("$.data.items[0].promotionStatus").value("ONGOING"))
				.andExpect(jsonPath("$.data.pagination.page").value(0))
				.andExpect(jsonPath("$.data.pagination.size").value(10));
		}

		@Test
		@DisplayName("상태 필터와 함께 조회하면 필터링된 결과를 반환한다")
		void getPromotionList_withStatusFilter_returnsFilteredResult() throws Exception {
			given(promotionService.getPromotionList(any(), any())).willReturn(createMockListResponse());

			mockMvc.perform(get("/api/v1/promotions")
				.param("page", "0")
				.param("size", "10")
				.param("promotionStatus", "ONGOING"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items").isArray());
		}
	}

	@Nested
	@DisplayName("프로모션 상세 조회")
	class GetPromotionDetail {

		@Test
		@DisplayName("프로모션 상세를 조회하면 상세 정보를 반환한다")
		void getPromotionDetail_returnsDetailInfo() throws Exception {
			given(promotionService.getPromotionDetail(anyLong())).willReturn(createMockDetailResponse());

			mockMvc.perform(get("/api/v1/promotions/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.title").value("신년 특가"))
				.andExpect(jsonPath("$.data.content").value("새해 맞이 할인 행사입니다."))
				.andExpect(jsonPath("$.data.bannerImageUrl").value("https://example.com/banner.jpg"))
				.andExpect(jsonPath("$.data.detailImageUrls").isArray())
				.andExpect(jsonPath("$.data.detailImageUrls[0]").value("https://example.com/detail1.jpg"))
				.andExpect(jsonPath("$.data.detailImageUrls[1]").value("https://example.com/detail2.jpg"));
		}
	}
}
