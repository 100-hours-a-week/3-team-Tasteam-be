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
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.admin.dto.request.AdminRestaurantCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminRestaurantSearchCondition;
import com.tasteam.domain.admin.dto.request.AdminRestaurantUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminRestaurantDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminRestaurantListItem;
import com.tasteam.domain.admin.service.AdminRestaurantService;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

@ControllerWebMvcTest(AdminRestaurantController.class)
@DisplayName("[유닛](Admin) AdminRestaurantController 단위 테스트")
class AdminRestaurantControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AdminRestaurantService adminRestaurantService;

	@Nested
	@DisplayName("음식점 목록 조회")
	class GetRestaurants {

		@Test
		@DisplayName("검색 조건으로 음식점 목록을 조회한다")
		void 음식점_목록_조회_성공() throws Exception {
			// given
			var item = new AdminRestaurantListItem(
				1L,
				"맛집이야기",
				"서울 강남구",
				List.of("한식", "분식"),
				Instant.parse("2026-02-01T10:00:00Z"),
				null);
			Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
			given(adminRestaurantService.getRestaurants(any(AdminRestaurantSearchCondition.class), any(Pageable.class)))
				.willReturn(new PageImpl<>(List.of(item), pageable, 1));

			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content[0].id").value(1))
				.andExpect(jsonPath("$.data.content[0].name").value("맛집이야기"));
		}

		@Test
		@DisplayName("페이지 파라미터가 숫자가 아니어도 기본값으로 목록을 조회한다")
		void 음식점_목록_페이지타입_오류_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants").param("page", "abc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("음식점 상세 조회")
	class GetRestaurant {

		@Test
		@DisplayName("ID로 음식점 상세 정보를 조회한다")
		void 음식점_상세_조회_성공() throws Exception {
			// given
			var response = new AdminRestaurantDetailResponse(
				1L,
				"맛집이야기",
				"서울 강남구",
				37.5,
				127.0,
				List.of(new AdminRestaurantDetailResponse.FoodCategoryInfo(10L, "한식", null)),
				List.of(new RestaurantImageDto(100L, "https://cdn.example.com/image.png")),
				Instant.parse("2026-01-01T10:00:00Z"),
				Instant.parse("2026-01-02T10:00:00Z"),
				null);
			given(adminRestaurantService.getRestaurantDetail(1L)).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.name").value("맛집이야기"));
		}

		@Test
		@DisplayName("음식점이 없으면 404로 실패한다")
		void 음식점_상세_미존재_실패() throws Exception {
			// given
			given(adminRestaurantService.getRestaurantDetail(999L))
				.willThrow(new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(RestaurantErrorCode.RESTAURANT_NOT_FOUND.name()));
		}
	}

	@Nested
	@DisplayName("음식점 등록")
	class CreateRestaurant {

		@Test
		@DisplayName("필수 정보로 음식점을 등록하면 ID를 반환한다")
		void 음식점_등록_성공() throws Exception {
			// given
			var request = new AdminRestaurantCreateRequest(
				"새 맛집",
				"서울시 강남구",
				"02-1234-5678",
				List.of(1L),
				List.of(UUID.fromString("6aa5f9e1-6f8f-4e2e-95c6-5f9d5de8e2e1")),
				List.of());
			given(adminRestaurantService.createRestaurant(request)).willReturn(10L);

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(10));
		}

		@Test
		@DisplayName("이름이 비면 400으로 실패한다")
		void 음식점_등록_이름_누락_실패() throws Exception {
			// given
			var request = new AdminRestaurantCreateRequest("", "서울시 강남구", null, List.of(), List.of(), List.of());

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("음식점 정보 수정")
	class UpdateRestaurant {

		@Test
		@DisplayName("수정 요청을 보내면 204를 반환한다")
		void 음식점_수정_성공() throws Exception {
			// given
			var request = new AdminRestaurantUpdateRequest(
				"수정 맛집",
				"서울시 서초구",
				List.of(2L),
				List.of(UUID.fromString("f2d3e4ad-98f8-4ce2-a1fd-8aafccfd3f6b")));
			doNothing().when(adminRestaurantService).updateRestaurant(1L, request);

			// when & then
			mockMvc.perform(patch("/api/v1/admin/restaurants/1")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		}

		@Test
		@DisplayName("음식점이 없으면 404로 실패한다")
		void 음식점_수정_미존재_실패() throws Exception {
			// given
			var request = new AdminRestaurantUpdateRequest("수정 맛집", null, List.of(), List.of());
			doThrow(new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND))
				.when(adminRestaurantService)
				.updateRestaurant(999L, request);

			// when & then
			mockMvc.perform(patch("/api/v1/admin/restaurants/999")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(RestaurantErrorCode.RESTAURANT_NOT_FOUND.name()));
		}
	}

	@Nested
	@DisplayName("음식점 삭제")
	class DeleteRestaurant {

		@Test
		@DisplayName("음식점 삭제하면 본문 없이 204를 반환한다")
		void 음식점_삭제_성공() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/admin/restaurants/1"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		}

		@Test
		@DisplayName("음식점이 없으면 404로 실패한다")
		void 음식점_삭제_미존재_실패() throws Exception {
			// given
			doThrow(new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND))
				.when(adminRestaurantService)
				.deleteRestaurant(999L);

			// when & then
			mockMvc.perform(delete("/api/v1/admin/restaurants/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(RestaurantErrorCode.RESTAURANT_NOT_FOUND.name()));
		}
	}
}
