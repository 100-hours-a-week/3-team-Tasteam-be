package com.tasteam.domain.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.auth.dto.request.TestAuthTokenRequest;
import com.tasteam.domain.auth.service.TestAuthTokenService;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;

@ActiveProfiles("test")
@ControllerWebMvcTest(TestAuthController.class)
@DisplayName("[유닛](Auth) TestAuthController 단위 테스트")
class TestAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private TestAuthTokenService testAuthTokenService;

	@MockitoBean
	private JwtCookieProvider jwtCookieProvider;

	@Test
	@DisplayName("테스트 사용자 토큰 발급이 성공하면 액세스 토큰을 반환한다")
	void 테스트_토큰_발급_성공() throws Exception {
		// given
		var request = new TestAuthTokenRequest("test-user-001", "부하테스트계정");
		given(testAuthTokenService.issueTokens(request.identifier(), request.nickname()))
			.willReturn(new TestAuthTokenService.TestTokenResult("access-token", "refresh-token", 7L, true));

		// when & then
		mockMvc.perform(post("/api/v1/auth/token/test")
			.contentType(APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.memberId").value(7))
			.andExpect(jsonPath("$.data.isNew").value(true));
	}

	@Test
	@DisplayName("식별자가 비면 400으로 실패한다")
	void 테스트_토큰_발급_식별자_누락_실패() throws Exception {
		// given
		var request = new TestAuthTokenRequest("", "");

		// when & then
		mockMvc.perform(post("/api/v1/auth/token/test")
			.contentType(APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("회원을 찾을 수 없으면 404으로 실패한다")
	void 테스트_토큰_발급_회원_없음_실패() throws Exception {
		// given
		var request = new TestAuthTokenRequest("missing-user", "부하테스트계정");
		given(testAuthTokenService.issueTokens(anyString(), anyString()))
			.willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/auth/token/test")
			.contentType(APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}
}
