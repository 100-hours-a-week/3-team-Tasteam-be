package com.tasteam.domain.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.controller.docs.AdminAuthControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminLoginRequest;
import com.tasteam.domain.admin.dto.response.AdminLoginResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController implements AdminAuthControllerDocs {

	private final JwtTokenProvider jwtTokenProvider;

	@Value("${tasteam.admin.username}")
	private String adminUsername;

	@Value("${tasteam.admin.password}")
	private String adminPassword;

	@Override
	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminLoginResponse> login(
		@Validated @RequestBody
		AdminLoginRequest request) {

		if (!adminUsername.equals(request.username()) || !adminPassword.equals(request.password())) {
			throw new BusinessException(AuthErrorCode.INVALID_ADMIN_CREDENTIALS);
		}

		String accessToken = jwtTokenProvider.generateAccessToken(0L, "ADMIN");

		return SuccessResponse.success(new AdminLoginResponse(accessToken));
	}
}
