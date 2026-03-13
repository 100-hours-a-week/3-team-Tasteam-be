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

import com.tasteam.config.BaseControllerWebMvcTest;
import com.tasteam.domain.auth.service.TokenRefreshService;

@DisplayName("[유닛](Auth) AuthController 단위 테스트")
class AuthControllerTest extends BaseControllerWebMvcTest {

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
