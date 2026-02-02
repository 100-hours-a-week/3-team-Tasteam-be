package com.tasteam.domain.restaurant.controller;

import static org.mockito.ArgumentMatchers.eq;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.restaurant.dto.response.MenuCategoryResponse;
import com.tasteam.domain.restaurant.dto.response.MenuItemResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.domain.restaurant.service.MenuService;

@ControllerWebMvcTest(MenuController.class)
class MenuControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MenuService menuService;

	@Nested
	@DisplayName("음식점 메뉴 조회")
	class GetRestaurantMenus {

		@Test
		@DisplayName("음식점 메뉴를 조회하면 카테고리별 메뉴 목록을 반환한다")
		void 메뉴_조회_성공() throws Exception {
			// given
			RestaurantMenuResponse response = new RestaurantMenuResponse(1L, List.of(
				new MenuCategoryResponse(1L, "메인메뉴", 0, List.of(
					new MenuItemResponse(1L, "된장찌개", "구수한 된장찌개", 9000, null, true, 0),
					new MenuItemResponse(2L, "김치찌개", "매콤한 김치찌개", 8000, null, false, 1)))));

			given(menuService.getRestaurantMenus(eq(1L), eq(false), eq(true))).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menus", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.restaurantId").value(1))
				.andExpect(jsonPath("$.data.categories[0].name").value("메인메뉴"))
				.andExpect(jsonPath("$.data.categories[0].menus[0].name").value("된장찌개"))
				.andExpect(jsonPath("$.data.categories[0].menus[0].price").value(9000));
		}

		@Test
		@DisplayName("빈 카테고리 포함 옵션으로 메뉴를 조회할 수 있다")
		void 빈_카테고리_포함_조회() throws Exception {
			// given
			RestaurantMenuResponse response = new RestaurantMenuResponse(1L, List.of(
				new MenuCategoryResponse(1L, "메인메뉴", 0, List.of()),
				new MenuCategoryResponse(2L, "사이드", 1, List.of())));

			given(menuService.getRestaurantMenus(eq(1L), eq(true), eq(true))).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/menus", 1L)
				.param("includeEmptyCategories", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.categories").isArray());
		}
	}

}
