package com.tasteam.infra.webhook.event;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

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
	String exceptionClass,
	String stackTrace) {

	public static ErrorContext from(BusinessException e, HttpServletRequest request) {
		return from(e, request, true);
	}

	public static ErrorContext from(BusinessException e, HttpServletRequest request, boolean includeStackTrace) {
		String traceId = resolveTraceId(request);
		String message = withTraceId(maskSensitive(e.getMessage()), traceId);
		return new ErrorContext(
			"BUSINESS",
			e.getErrorCode().toString(),
			message,
			e.getHttpStatus(),
			request != null ? request.getMethod() : "UNKNOWN",
			request != null ? request.getRequestURI() : "UNKNOWN",
			request != null ? Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown") : "Unknown",
			Instant.now(),
			e.getClass().getSimpleName(),
			resolveStackTrace(e, includeStackTrace));
	}

	public static ErrorContext from(Exception e, HttpServletRequest request) {
		return from(e, request, true);
	}

	public static ErrorContext from(Exception e, HttpServletRequest request, boolean includeStackTrace) {
		String traceId = resolveTraceId(request);
		String message = withTraceId(maskSensitive(coreMessage(e)), traceId);
		return new ErrorContext(
			"SYSTEM",
			"INTERNAL_SERVER_ERROR",
			message,
			HttpStatus.INTERNAL_SERVER_ERROR,
			request != null ? request.getMethod() : "UNKNOWN",
			request != null ? request.getRequestURI() : "UNKNOWN",
			request != null ? Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown") : "Unknown",
			Instant.now(),
			e.getClass().getSimpleName(),
			resolveStackTrace(e, includeStackTrace));
	}

	public static ErrorContext fromValidation(Exception e, HttpServletRequest request) {
		return fromValidation(e, request, true);
	}

	public static ErrorContext fromValidation(Exception e, HttpServletRequest request, boolean includeStackTrace) {
		String traceId = resolveTraceId(request);
		String message = withTraceId("요청 값이 올바르지 않습니다.", traceId);
		return new ErrorContext(
			"VALIDATION",
			"INVALID_REQUEST",
			message,
			HttpStatus.BAD_REQUEST,
			request != null ? request.getMethod() : "UNKNOWN",
			request != null ? request.getRequestURI() : "UNKNOWN",
			request != null ? Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown") : "Unknown",
			Instant.now(),
			e.getClass().getSimpleName(),
			resolveStackTrace(e, includeStackTrace));
	}

	public static ErrorContext fromSecurity(Exception e, HttpServletRequest request, HttpStatus httpStatus,
		String errorCode) {
		return fromSecurity(e, request, httpStatus, errorCode, true);
	}

	public static ErrorContext fromSecurity(Exception e, HttpServletRequest request, HttpStatus httpStatus,
		String errorCode, boolean includeStackTrace) {
		String traceId = resolveTraceId(request);
		String message = withTraceId(maskSensitive(coreMessage(e)), traceId);
		return new ErrorContext(
			"SECURITY",
			errorCode,
			message,
			httpStatus,
			request != null ? request.getMethod() : "UNKNOWN",
			request != null ? request.getRequestURI() : "UNKNOWN",
			request != null ? Optional.ofNullable(request.getHeader("User-Agent")).orElse("Unknown") : "Unknown",
			Instant.now(),
			e.getClass().getSimpleName(),
			resolveStackTrace(e, includeStackTrace));
	}

	private static String resolveStackTrace(Throwable throwable, boolean includeStackTrace) {
		if (!includeStackTrace) {
			return null;
		}
		return getStackTraceAsString(throwable);
	}

	private static String getStackTraceAsString(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		return sw.toString();
	}

	private static String coreMessage(Exception exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getName();
		}
		return exception.getClass().getName() + ": " + message;
	}

	private static String resolveTraceId(HttpServletRequest request) {
		if (request == null) {
			return UUID.randomUUID().toString();
		}

		String traceId = Optional.ofNullable(request.getHeader("X-Trace-Id"))
			.orElseGet(() -> request.getHeader("X-Request-Id"));
		if (traceId == null || traceId.isBlank()) {
			return UUID.randomUUID().toString();
		}
		return traceId;
	}

	private static String withTraceId(String message, String traceId) {
		String baseMessage = message == null || message.isBlank() ? "Unknown" : message;
		return baseMessage + " [traceId=" + traceId + "]";
	}

	private static String maskSensitive(String message) {
		if (message == null || message.isBlank()) {
			return message;
		}

		String masked = message;
		masked = EMAIL_PATTERN.matcher(masked).replaceAll("<redacted-email>");
		masked = PHONE_PATTERN.matcher(masked).replaceAll("<redacted-phone>");
		masked = BEARER_PATTERN.matcher(masked).replaceAll("Bearer <redacted>");
		masked = JWT_PATTERN.matcher(masked).replaceAll("<redacted-jwt>");
		masked = COOKIE_PATTERN.matcher(masked).replaceAll("$1<redacted>");
		masked = AUTH_HEADER_PATTERN.matcher(masked).replaceAll("$1<redacted>");
		return masked;
	}

	private static boolean isLocalOrDev() {
		String activeProfiles = Optional.ofNullable(System.getProperty("spring.profiles.active"))
			.orElseGet(() -> System.getenv("SPRING_PROFILES_ACTIVE"));
		if (activeProfiles == null || activeProfiles.isBlank()) {
			return false;
		}
		return Arrays.stream(activeProfiles.split("[,\\s]+"))
			.anyMatch(profile -> "local".equalsIgnoreCase(profile) || "dev".equalsIgnoreCase(profile));
	}

	private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
	private static final Pattern PHONE_PATTERN = Pattern
		.compile("\\b(?:\\+?\\d{1,3}[\\s-]?)?(?:\\d{2,4}[\\s-]?)?\\d{3,4}[\\s-]?\\d{4}\\b");
	private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer\\s+[A-Za-z0-9._\\-]+");
	private static final Pattern JWT_PATTERN = Pattern
		.compile("\\b[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_=]+\\b");
	private static final Pattern COOKIE_PATTERN = Pattern.compile("(?i)(cookie\\s*[:=]\\s*)([^;\\s]+)");
	private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile("(?i)(authorization\\s*[:=]\\s*)([^\\s]+)");
}
