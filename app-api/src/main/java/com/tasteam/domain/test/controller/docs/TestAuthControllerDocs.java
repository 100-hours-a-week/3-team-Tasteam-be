package com.tasteam.domain.test.controller.docs;

import com.tasteam.domain.auth.dto.request.TestAuthTokenRequest;
import com.tasteam.domain.auth.dto.response.TestAuthTokenResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@SwaggerTagOrder(5)
@Tag(name = "Auth", description = "인증 관련 API")
public interface TestAuthControllerDocs {

	@Operation(summary = "테스트 로그인 토큰 발급", description = "부하 테스트 및 개발용 백도어 로그인 API입니다. identifier로 TEST OAuth 계정을 조회해 기존 회원을 로그인시키고, 계정이 없으면 회원 가입 후 로그인합니다. dev, local, stg 프로필에서만 사용 가능하며 stg 기본 만료시간은 48시간입니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = TestAuthTokenRequest.class), examples = @ExampleObject(name = "테스트 토큰 발급 요청", value = """
		{
		    "identifier": "test-user-001",
		    "nickname": "부하테스트계정1"
		}
		""")))
	@ApiResponse(responseCode = "200", description = "토큰 발급 성공", content = @Content(schema = @Schema(implementation = TestAuthTokenResponse.class), examples = @ExampleObject(name = "토큰 발급 응답", value = """
		{
		    "data": {
		        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
		        "memberId": 123,
		        "isNew": true
		    }
		}
		""")))
	SuccessResponse<TestAuthTokenResponse> issueTestToken(@Valid
	TestAuthTokenRequest request, HttpServletResponse servletResponse);
}
