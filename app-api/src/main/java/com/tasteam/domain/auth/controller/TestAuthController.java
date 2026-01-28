package com.tasteam.domain.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.auth.controller.docs.TestAuthControllerDocs;
import com.tasteam.domain.auth.dto.request.TestAuthTokenRequest;
import com.tasteam.domain.auth.dto.response.TestAuthTokenResponse;
import com.tasteam.domain.auth.service.TestAuthTokenService;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Profile({"dev", "test"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class TestAuthController implements TestAuthControllerDocs {

	private final TestAuthTokenService testAuthTokenService;

	@Override
	@PostMapping("/token/test")
	public SuccessResponse<TestAuthTokenResponse> issueTestToken(
		@Valid @RequestBody
		TestAuthTokenRequest request) {
		TestAuthTokenService.TestTokenResult result = testAuthTokenService.issueTokens(
			request.identifier(),
			request.nickname());

		TestAuthTokenResponse response = new TestAuthTokenResponse(
			result.accessToken(),
			result.memberId(),
			result.isNew());

		return SuccessResponse.success(response);
	}
}
