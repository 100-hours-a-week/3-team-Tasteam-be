package com.tasteam.domain.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseAdminControllerWebMvcTest;
import com.tasteam.domain.restaurant.dto.request.MenuBulkCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCategoryCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCreateRequest;
import com.tasteam.domain.restaurant.dto.response.MenuCategoryResponse;
import com.tasteam.domain.restaurant.dto.response.MenuItemResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;

@DisplayName("[유닛](Admin) AdminMenuController 단위 테스트")
class AdminMenuControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("메뉴 조회")
	class GetMenus {

		@Test
		@DisplayName("식당 메뉴를 조회하면 카테고리와 메뉴 목록을 반환한다")
		void 메뉴_조회_성공() throws Exception {
			// given
			var response = new RestaurantMenuResponse(1L, List.of(
				new MenuCategoryResponse(11L, "메인", 0, List.of(
					new MenuItemResponse(101L, 1L, "된장찌개", "구수한 국물", 9000, null, true, 0)))));
			given(menuService.getRestaurantMenus(1L, true, false)).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants/1/menus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.restaurantId").value(1))
				.andExpect(jsonPath("$.data.categories").isArray())
				.andExpect(jsonPath("$.data.categories[0].id").value(11))
				.andExpect(jsonPath("$.data.categories[0].menus[0].name").value("된장찌개"));
		}

		@Test
		@DisplayName("식당 ID가 숫자가 아니면 400으로 실패한다")
		void 메뉴_조회_경로변수_유형_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants/abc/menus"))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("메뉴 카테고리 생성")
	class CreateCategory {

		@Test
		@DisplayName("유효한 요청이면 메뉴 카테고리 ID를 반환한다")
		void 메뉴카테고리_생성_성공() throws Exception {
			// given
			var request = new MenuCategoryCreateRequest("메인", 0);
			given(menuService.createMenuCategory(1L, request)).willReturn(10L);

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/1/menus/categories")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(10));
		}

		@Test
		@DisplayName("이름이 비면 400으로 실패한다")
		void 메뉴카테고리_생성_이름_누락_실패() throws Exception {
			// given
			var request = new MenuCategoryCreateRequest("", 0);

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/1/menus/categories")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("메뉴 생성")
	class CreateMenu {

		@Test
		@DisplayName("유효한 요청이면 메뉴 ID를 반환한다")
		void 메뉴_생성_성공() throws Exception {
			// given
			var request = new MenuCreateRequest(1L, "된장찌개", "구수한 된장", 9000, null, null, true, 0);
			given(menuService.createMenu(1L, request)).willReturn(20L);

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/1/menus")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(20));
		}

		@Test
		@DisplayName("카테고리 ID가 없으면 400으로 실패한다")
		void 메뉴_생성_카테고리누락_실패() throws Exception {
			// given
			var request = new MenuCreateRequest(null, "된장찌개", "구수한 된장", 9000, null, null, true, 0);

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/1/menus")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("메뉴 일괄 생성")
	class CreateMenusBulk {

		@Test
		@DisplayName("유효한 다건 요청이면 성공 응답을 반환한다")
		void 메뉴_일괄_생성_성공() throws Exception {
			// given
			var request = new MenuBulkCreateRequest(List.of(
				new MenuCreateRequest(1L, "된장찌개", "구수한 된장", 9000, null, null, true, 0),
				new MenuCreateRequest(1L, "김치찌개", "매운 김치", 8000, null, null, false, 1)));
			given(menuService.createMenusBulk(1L, request)).willReturn(List.of(30L, 31L));

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/1/menus/bulk")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("메뉴 목록이 비면 400으로 실패한다")
		void 메뉴_일괄_생성_목록_누락_실패() throws Exception {
			// given
			var request = new MenuBulkCreateRequest(List.of());

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/1/menus/bulk")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}
}
