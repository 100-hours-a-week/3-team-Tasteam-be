package com.tasteam.domain.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseAdminControllerWebMvcTest;
import com.tasteam.domain.admin.dto.request.AdminFoodCategoryCreateRequest;

@DisplayName("[유닛](Admin) AdminFoodCategoryController 단위 테스트")
class AdminFoodCategoryControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("음식 카테고리 생성")
	class CreateFoodCategory {

		@Test
		@DisplayName("유효한 요청이면 카테고리 ID를 반환한다")
		void 카테고리_생성_성공() throws Exception {
			// given
			var request = new AdminFoodCategoryCreateRequest("한식");
			given(foodCategoryService.createFoodCategory(request.name())).willReturn(10L);

			// when & then
			mockMvc.perform(post("/api/v1/admin/food-categories")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(10));
		}

		@Test
		@DisplayName("이름이 비면 400으로 실패한다")
		void 카테고리_생성_이름_누락_실패() throws Exception {
			// given
			var request = new AdminFoodCategoryCreateRequest("");

			// when & then
			mockMvc.perform(post("/api/v1/admin/food-categories")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}
}
