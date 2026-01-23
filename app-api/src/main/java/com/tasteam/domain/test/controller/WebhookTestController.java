package com.tasteam.domain.test.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.MemberErrorCode;

@RestController
@RequestMapping("/api/v1/test")
public class WebhookTestController {

	@GetMapping("/error/business")
	public String testBusinessException() {
		throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
	}

	@GetMapping("/error/system")
	public String testSystemException() {
		throw new RuntimeException("테스트용 시스템 예외 발생");
	}

	@GetMapping("/error/null-pointer")
	public String testNullPointerException() {
		String value = null;
		return value.toString();
	}

	@GetMapping("/error/arithmetic")
	public String testArithmeticException() {
		int result = 10 / 0;
		return "result: " + result;
	}
}
