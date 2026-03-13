package com.tasteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.tasteam.config.BaseAdminControllerWebMvcTest;
import com.tasteam.domain.admin.dto.request.AdminPromotionCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminPromotionUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminPromotionDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminPromotionListItem;
import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.DisplayStatus;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.entity.PublishStatus;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.PromotionErrorCode;

@DisplayName("[유닛](Admin) AdminPromotionController 단위 테스트")
class AdminPromotionControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("프로모션 목록 조회")
	class GetPromotions {

		@Test
		@DisplayName("프로모션 목록을 조회하면 페이징 결과를 반환한다")
		void 프로모션_목록_조회_성공() throws Exception {
			// given
			AdminPromotionListItem item = new AdminPromotionListItem(
				1L,
				"신규 가입 이벤트",
				PromotionStatus.ONGOING,
				DisplayStatus.DISPLAYING,
				PublishStatus.PUBLISHED,
				Instant.parse("2026-02-01T00:00:00Z"),
				Instant.parse("2026-02-02T00:00:00Z"),
				DisplayChannel.MAIN_BANNER,
				"https://cdn.example.com/banner.png",
				Instant.parse("2026-01-31T00:00:00Z"));
			Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
			Page<AdminPromotionListItem> page = new PageImpl<>(List.of(item), pageable, 1);
			given(adminPromotionService.getPromotionList(any(), any(), any(), any(Pageable.class))).willReturn(page);

			// when & then
			mockMvc.perform(get("/api/v1/admin/promotions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content[0].id").value(1))
				.andExpect(jsonPath("$.data.content[0].title").value("신규 가입 이벤트"));
		}

		@Test
		@DisplayName("페이지 파라미터가 숫자가 아니어도 기본값으로 조회를 수행한다")
		void 프로모션_목록_페이지타입_오류_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/promotions").param("page", "abc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("프로모션 상세 조회")
	class GetPromotion {

		@Test
		@DisplayName("프로모션 ID로 상세 정보를 조회한다")
		void 프로모션_상세_조회_성공() throws Exception {
			// given
			given(adminPromotionService.getPromotionDetail(1L)).willReturn(new AdminPromotionDetailResponse(
				1L,
				"신규 가입 이벤트",
				"지금 가입하면 쿠폰 지급",
				"https://example.com/event",
				Instant.parse("2026-02-01T00:00:00Z"),
				Instant.parse("2026-03-01T00:00:00Z"),
				PublishStatus.PUBLISHED,
				com.tasteam.domain.promotion.entity.PromotionStatus.ONGOING,
				true,
				Instant.parse("2026-01-31T00:00:00Z"),
				Instant.parse("2026-02-01T00:00:00Z"),
				DisplayChannel.BOTH,
				1,
				DisplayStatus.DISPLAYING,
				"https://cdn.example.com/banner.png",
				"https://cdn.example.com/splash.png",
				"배너",
				List.of("https://cdn.example.com/detail-1.png"),
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-02T00:00:00Z")));

			// when & then
			mockMvc.perform(get("/api/v1/admin/promotions/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.title").value("신규 가입 이벤트"));
		}

		@Test
		@DisplayName("프로모션이 없으면 404로 실패한다")
		void 프로모션_상세_미존재_실패() throws Exception {
			// given
			given(adminPromotionService.getPromotionDetail(999L))
				.willThrow(new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/api/v1/admin/promotions/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(PromotionErrorCode.PROMOTION_NOT_FOUND.name()));
		}
	}

	@Nested
	@DisplayName("프로모션 등록")
	class CreatePromotion {

		@Test
		@DisplayName("필수 값을 넣으면 프로모션을 생성하고 ID를 반환한다")
		void 프로모션_생성_성공() throws Exception {
			// given
			var request = new AdminPromotionCreateRequest(
				"신규 가입 이벤트",
				"지금 가입하면 혜택",
				"https://example.com",
				Instant.parse("2026-02-01T00:00:00Z"),
				Instant.parse("2026-03-01T00:00:00Z"),
				PublishStatus.PUBLISHED,
				true,
				Instant.parse("2026-02-01T00:00:00Z"),
				Instant.parse("2026-02-05T00:00:00Z"),
				DisplayChannel.MAIN_BANNER,
				1,
				"https://cdn.example.com/banner.png",
				"https://cdn.example.com/splash.png",
				"배너 이미지",
				List.of("https://cdn.example.com/detail.png"));
			given(adminPromotionService.createPromotion(request)).willReturn(10L);

			// when & then
			mockMvc.perform(post("/api/v1/admin/promotions")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(10));
		}

		@Test
		@DisplayName("필수 값이 비면 400으로 실패한다")
		void 프로모션_생성_필수값_누락_실패() throws Exception {
			// given
			String body = "{}";

			// when & then
			mockMvc.perform(post("/api/v1/admin/promotions")
				.contentType(APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("프로모션 수정")
	class UpdatePromotion {

		@Test
		@DisplayName("수정 요청이 정상이면 200을 반환한다")
		void 프로모션_수정_성공() throws Exception {
			// given
			var request = new AdminPromotionUpdateRequest(
				"변경 제목",
				"변경 내용",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				1,
				null,
				null,
				null,
				null);
			doNothing().when(adminPromotionService).updatePromotion(1L, request);

			// when & then
			mockMvc.perform(patch("/api/v1/admin/promotions/1")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("프로모션이 없으면 404로 실패한다")
		void 프로모션_수정_미존재_실패() throws Exception {
			// given
			var request = new AdminPromotionUpdateRequest(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
			doThrow(new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND))
				.when(adminPromotionService).updatePromotion(999L, request);

			// when & then
			mockMvc.perform(patch("/api/v1/admin/promotions/999")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(PromotionErrorCode.PROMOTION_NOT_FOUND.name()));
		}
	}

	@Nested
	@DisplayName("프로모션 삭제")
	class DeletePromotion {

		@Test
		@DisplayName("프로모션이 존재하면 204로 삭제 응답을 반환한다")
		void 프로모션_삭제_성공() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/admin/promotions/1"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		}

		@Test
		@DisplayName("프로모션이 없으면 404로 실패한다")
		void 프로모션_삭제_미존재_실패() throws Exception {
			// given
			doThrow(new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND))
				.when(adminPromotionService).deletePromotion(999L);

			// when & then
			mockMvc.perform(delete("/api/v1/admin/promotions/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(PromotionErrorCode.PROMOTION_NOT_FOUND.name()));
		}
	}
}
