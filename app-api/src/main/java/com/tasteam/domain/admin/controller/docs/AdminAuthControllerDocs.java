package com.tasteam.domain.admin.controller.docs;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.admin.dto.request.AdminLoginRequest;
import com.tasteam.domain.admin.dto.response.AdminLoginResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(100)
@Tag(name = "Admin - Auth", description = "어드민 인증 API")
public interface AdminAuthControllerDocs {

	@Operation(summary = "어드민 로그인", description = "어드민 계정으로 로그인하여 액세스 토큰을 발급받습니다.")
	SuccessResponse<AdminLoginResponse> login(
		@Validated @RequestBody
		AdminLoginRequest request);
}
