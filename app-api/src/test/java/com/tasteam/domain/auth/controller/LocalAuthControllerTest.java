package com.tasteam.domain.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.auth.dto.request.LocalAuthTokenRequest;
import com.tasteam.domain.auth.service.LocalAuthTokenService;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;
import com.tasteam.infra.ai.AiClient;

@ActiveProfiles("local")
@ControllerWebMvcTest(LocalAuthController.class)
@DisplayName("[유닛](Auth) LocalAuthController 단위 테스트")
class LocalAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private LocalAuthTokenService localAuthTokenService;

	@MockitoBean
	private JwtCookieProvider jwtCookieProvider;

	@MockitoBean
	private AiClient aiClient;

	@Nested
	@DisplayName("로컬 토큰 발급")
	class IssueLocalToken {

		@Test
		@DisplayName("유효한 요청이면 로컬 토큰을 발급하고 200을 반환한다")
		void 로컬_토큰_발급_성공() throws Exception {
			// given
			var request = new LocalAuthTokenRequest("dev@example.com", "개발자");
			given(localAuthTokenService.issueTokens(request.email(), request.nickname()))
				.willReturn(new LocalAuthTokenService.TokenPair("access-token", "refresh-token", 1L));

			// when & then
			mockMvc.perform(post("/api/v1/auth/token")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value("access-token"))
				.andExpect(jsonPath("$.data.memberId").value(1));
		}

		@Test
		@DisplayName("이메일 형식이 잘못되면 400으로 실패한다")
		void 로컬_토큰_발급_잘못된_이메일_실패() throws Exception {
			// given
			var request = new LocalAuthTokenRequest("invalid-email", "개발자");

			// when & then
			mockMvc.perform(post("/api/v1/auth/token")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("AI 헬스체크")
	class CheckAiHealth {

		@Test
		@DisplayName("AI 헬스체크가 성공하면 success 응답을 반환한다")
		void ai_헬스체크_성공() throws Exception {
			// given
			// when & then
			mockMvc.perform(post("/api/v1/auth/ai/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("AI 헬스체크가 실패하면 500으로 실패한다")
		void ai_헬스체크_실패() throws Exception {
			// given
			var failure = new RuntimeException("ai is down");
			org.mockito.BDDMockito.willThrow(failure).given(aiClient).healthCheck();

			// when & then
			mockMvc.perform(post("/api/v1/auth/ai/health"))
				.andExpect(status().isInternalServerError());
		}
	}
}
