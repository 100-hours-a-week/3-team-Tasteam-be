package com.tasteam.domain.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseAdminControllerWebMvcTest;

@DisplayName("[유닛](Admin) AdminSpaFallbackController 단위 테스트")
class AdminSpaFallbackControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("관리자 SPA 직접 라우팅")
	class AdminSpaRoutes {

		@Test
		@DisplayName("루트 경로는 admin/index.html로 포워드한다")
		void 관리자_루트_경로_포워드() throws Exception {
			// when & then
			mockMvc.perform(get("/admin"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/admin/index.html"));
		}

		@Test
		@DisplayName("admin/ 경로도 admin/index.html로 포워드한다")
		void 관리자_루트_슬래시_포워드() throws Exception {
			// when & then
			mockMvc.perform(get("/admin/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/admin/index.html"));
		}

		@Test
		@DisplayName("admin/pages 경로는 admin/index.html로 포워드한다")
		void 관리자_페이지_경로_포워드() throws Exception {
			// when & then
			mockMvc.perform(get("/admin/pages"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/admin/index.html"));
		}
	}

	@Nested
	@DisplayName("/admin/pages 하위 경로 포워드")
	class AdminPageRoutes {

		@Test
		@DisplayName("admin/pages 이하 경로는 admin/index.html로 포워드한다")
		void 페이지_하위경로_포워드() throws Exception {
			// when & then
			mockMvc.perform(get("/admin/pages/user/list"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/admin/index.html"));
		}

		@Test
		@DisplayName("admin/pages/index.html도 admin/index.html로 포워드한다")
		void 페이지_정적파일_포워드() throws Exception {
			// when & then
			mockMvc.perform(get("/admin/pages/index.html"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/admin/index.html"));
		}
	}
}
