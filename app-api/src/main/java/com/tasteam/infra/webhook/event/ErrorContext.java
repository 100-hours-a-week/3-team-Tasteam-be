package com.tasteam.infra.webhook.event;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.business.BusinessException;

import jakarta.servlet.http.HttpServletRequest;

public record ErrorContext(
	String errorType,
	String errorCode,
	String message,
	HttpStatus httpStatus,
	String requestMethod,
	String requestPath,
	String userAgent,
	Instant timestamp,
	String exceptionClass) {

	public static ErrorContext from(BusinessException e, HttpServletRequest request) {
		return new ErrorContext(
			"BUSINESS",
			e.getErrorCode().toString(),
			e.getMessage(),
			e.getHttpStatus(),
			request != null ? request.getMethod() : "UNKNOWN",
			request != null ? request.getRequestURI() : "UNKNOWN",
			request != null ? Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown") : "Unknown",
			Instant.now(),
			e.getClass().getSimpleName());
	}

	public static ErrorContext from(Exception e, HttpServletRequest request) {
		return new ErrorContext(
			"SYSTEM",
			"INTERNAL_SERVER_ERROR",
			e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
			HttpStatus.INTERNAL_SERVER_ERROR,
			request != null ? request.getMethod() : "UNKNOWN",
			request != null ? request.getRequestURI() : "UNKNOWN",
			request != null ? Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown") : "Unknown",
			Instant.now(),
			e.getClass().getSimpleName());
	}

	public static ErrorContext fromValidation(Exception e, HttpServletRequest request) {
		return new ErrorContext(
			"VALIDATION",
			"INVALID_REQUEST",
			"요청 값이 올바르지 않습니다.",
			HttpStatus.BAD_REQUEST,
			request != null ? request.getMethod() : "UNKNOWN",
			request != null ? request.getRequestURI() : "UNKNOWN",
			request != null ? Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown") : "Unknown",
			Instant.now(),
			e.getClass().getSimpleName());
	}
}
