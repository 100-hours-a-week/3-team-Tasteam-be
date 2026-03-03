package com.tasteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.admin.dto.request.AdminLoginRequest;
import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;

@TestPropertySource(properties = {
	"tasteam.admin.username=admin",
	"tasteam.admin.password=pass1234!"
})
@ControllerWebMvcTest(AdminAuthController.class)
@DisplayName("[유닛](Admin) AdminAuthController 단위 테스트")
class AdminAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private JwtCookieProvider jwtCookieProvider;

	@Nested
	@DisplayName("관리자 로그인")
	class Login {

		@Test
		@DisplayName("관리자 계정 정보가 정확하면 토큰을 반환한다")
		void 로그인_성공() throws Exception {
			// given
			var request = new AdminLoginRequest("admin", "pass1234!");
			given(jwtTokenProvider.generateAccessToken(0L, "ADMIN")).willReturn("access-token");
			doNothing().when(jwtCookieProvider).addAdminAccessTokenCookie(any(HttpServletResponse.class),
				eq("access-token"));

			// when & then
			mockMvc.perform(post("/api/v1/admin/auth/login")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value("access-token"));

			verify(jwtCookieProvider).addAdminAccessTokenCookie(any(HttpServletResponse.class), eq("access-token"));
		}

		@Test
		@DisplayName("필수 필드 누락이면 400으로 실패한다")
		void 로그인_요청_누락_실패() throws Exception {
			// given
			String body = "{\"username\":\"admin\"}";

			// when & then
			mockMvc.perform(post("/api/v1/admin/auth/login")
				.contentType(APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}

		@Test
		@DisplayName("관리자 계정 정보가 다르면 401로 실패한다")
		void 로그인_자격증명_불일치_실패() throws Exception {
			// given
			var request = new AdminLoginRequest("admin", "wrong");

			// when & then
			mockMvc.perform(post("/api/v1/admin/auth/login")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ADMIN_CREDENTIALS.name()));
		}
	}
}
