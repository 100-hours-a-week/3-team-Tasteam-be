package com.tasteam.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.auth.service.TokenRefreshService;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;

@ControllerWebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TokenRefreshService tokenRefreshService;

	@MockitoBean
	private JwtCookieProvider jwtCookieProvider;

	@Nested
	@DisplayName("토큰 갱신")
	class RefreshToken {

		@Test
		@DisplayName("리프레시 토큰으로 새 액세스 토큰을 발급받는다")
		void 토큰_갱신_성공() throws Exception {
			// given
			TokenRefreshService.TokenPair tokenPair = new TokenRefreshService.TokenPair(
				"new-access-token", "new-refresh-token");

			given(tokenRefreshService.refreshTokens(any())).willReturn(tokenPair);
			willDoNothing().given(jwtCookieProvider).addRefreshTokenCookie(any(), any());

			// when & then
			mockMvc.perform(post("/api/v1/auth/token/refresh"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
		}
	}
}
