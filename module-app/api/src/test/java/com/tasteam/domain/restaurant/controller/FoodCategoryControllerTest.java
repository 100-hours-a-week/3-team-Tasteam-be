package com.tasteam.domain.restaurant.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseControllerWebMvcTest;
import com.tasteam.domain.restaurant.dto.response.FoodCategoryResponse;

@DisplayName("[유닛](Restaurant) FoodCategoryController 단위 테스트")
class FoodCategoryControllerTest extends BaseControllerWebMvcTest {

	@Test
	@DisplayName("음식 카테고리 목록을 조회하면 카테고리 목록을 반환한다")
	void 음식카테고리_조회_성공() throws Exception {
		// given
		given(foodCategoryService.getFoodCategories())
			.willReturn(List.of(new FoodCategoryResponse(1L, "한식"), new FoodCategoryResponse(2L, "중식")));

		// when & then
		mockMvc.perform(get("/api/v1/food-categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].id").value(1))
			.andExpect(jsonPath("$.data[0].name").value("한식"));
	}

	@Test
	@DisplayName("서비스 장애 시 500으로 실패한다")
	void 음식카테고리_조회_서비스_오류_실패() throws Exception {
		// given
		given(foodCategoryService.getFoodCategories()).willThrow(new RuntimeException("service down"));

		// when & then
		mockMvc.perform(get("/api/v1/food-categories"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.success").value(false));
	}
}
