package com.tasteam.global.security.jwt.provider;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.tasteam.global.security.common.constants.ApiEndpoints;
import com.tasteam.global.security.config.properties.SecurityCookieProperties;
import com.tasteam.global.security.jwt.common.JwtCookieConstants;
import com.tasteam.global.security.jwt.properties.JwtProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JWT 리프레시 토큰 쿠키 관리 제공자
 */
@Component
@RequiredArgsConstructor
public class JwtCookieProvider {

	private final JwtProperties jwtProperties;
	private final SecurityCookieProperties securityCookieProperties;

	public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		long maxAgeInSeconds = jwtProperties.getRefreshTokenExpiration() / 1000;
		addCookie(response, JwtCookieConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken,
			ApiEndpoints.AUTH, maxAgeInSeconds);
	}

	public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
		return getCookie(request, JwtCookieConstants.REFRESH_TOKEN_COOKIE_NAME);
	}

	public void deleteRefreshTokenCookie(HttpServletResponse response) {
		deleteCookie(response, JwtCookieConstants.REFRESH_TOKEN_COOKIE_NAME, ApiEndpoints.AUTH);
	}

	private void addCookie(HttpServletResponse response, String name, String value, String path, long maxAge) {
		ResponseCookie cookie = ResponseCookie.from(name, value)
			.secure(securityCookieProperties.isSecure())
			.httpOnly(true)
			.path(path)
			.maxAge(maxAge)
			.sameSite(JwtCookieConstants.SAME_SITE_STRICT)
			.build();

		response.addHeader(JwtCookieConstants.SET_COOKIE_HEADER, cookie.toString());
	}

	private Optional<String> getCookie(HttpServletRequest request, String name) {
		if (request.getCookies() == null) {
			return Optional.empty();
		}
		return Arrays.stream(request.getCookies())
			.filter(cookie -> name.equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst();
	}

	private void deleteCookie(HttpServletResponse response, String name, String path) {
		ResponseCookie cookie = ResponseCookie.from(name, "")
			.secure(securityCookieProperties.isSecure())
			.httpOnly(true)
			.path(path)
			.maxAge(0)
			.sameSite(JwtCookieConstants.SAME_SITE_STRICT)
			.build();

		response.addHeader(JwtCookieConstants.SET_COOKIE_HEADER, cookie.toString());
	}
}
