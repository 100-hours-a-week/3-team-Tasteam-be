package com.tasteam.global.security.oauth.cookie;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.tasteam.global.security.config.properties.SecurityCookieProperties;
import com.tasteam.global.security.jwt.properties.JwtProperties;
import com.tasteam.global.security.oauth.common.OAuth2CookieConstants;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 인증 요청 쿠키 관리 제공자
 */
@Component
@RequiredArgsConstructor
public class OAuth2CookieProvider {

	private final SecurityCookieProperties securityCookieProperties;
	private final JwtProperties jwtProperties;

	/**
	 * OAuth2 인증 요청 정보를 직렬화해서 쿠키로 저장합니다.
	 */
	public void addOAuth2AuthorizationRequestCookie(HttpServletResponse response, String value) {
		addCookie(
			response,
			OAuth2CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
			sign(value),
			"/",
			OAuth2CookieConstants.OAUTH2_COOKIE_EXPIRE_SECONDS);
	}

	/**
	 * 쿠키에서 OAuth2 인증 요청 데이터를 복원합니다.
	 */
	public Optional<String> getOAuth2AuthorizationRequestCookie(HttpServletRequest request) {
		return getCookie(request, OAuth2CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
			.flatMap(this::verifyAndExtract);
	}

	/**
	 * OAuth2 인증 요청 쿠키를 무효화합니다.
	 */
	public void deleteOAuth2AuthorizationRequestCookie(HttpServletResponse response) {
		deleteCookie(response, OAuth2CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, "/");
	}

	/**
	 * OAuth2 인증 중 리다이렉트할 URI를 쿠키에 저장합니다.
	 */
	public void addRedirectUriCookie(HttpServletResponse response, String redirectUri) {
		addCookie(
			response,
			OAuth2CookieConstants.REDIRECT_URI_COOKIE_NAME,
			sign(redirectUri),
			"/",
			OAuth2CookieConstants.OAUTH2_COOKIE_EXPIRE_SECONDS);
	}

	/**
	 * 저장된 OAuth2 리다이렉트 URI를 조회합니다.
	 */
	public Optional<String> getRedirectUriCookie(HttpServletRequest request) {
		return getCookie(request, OAuth2CookieConstants.REDIRECT_URI_COOKIE_NAME)
			.flatMap(this::verifyAndExtract);
	}

	/**
	 * OAuth2 리다이렉트 URI 쿠키를 삭제합니다.
	 */
	public void deleteRedirectUriCookie(HttpServletResponse response) {
		deleteCookie(response, OAuth2CookieConstants.REDIRECT_URI_COOKIE_NAME, "/");
	}

	//////////////////////////////////////////////////////////////////////////////

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
			String payload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(value.getBytes(StandardCharsets.UTF_8));
			String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
			return payload + "." + sig;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to sign cookie value", e);
		}
	}

	private Optional<String> verifyAndExtract(String signedValue) {
		try {
			String[] parts = signedValue.split("\\.");
			if (parts.length != 2) {
				return Optional.empty();
			}
			String payload = parts[0];
			String sig = parts[1];

			String expectedSigned = sign(new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8));
			if (!expectedSigned.endsWith("." + sig)) {
				return Optional.empty();
			}
			byte[] decoded = Base64.getUrlDecoder().decode(payload);
			return Optional.of(new String(decoded, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private void addCookie(HttpServletResponse response, String name, String value, String path, long maxAge) {
		ResponseCookie cookie = ResponseCookie.from(name, value)
			.secure(securityCookieProperties.isSecure())
			.httpOnly(true)
			.path(path)
			.maxAge(maxAge)
			.sameSite(securityCookieProperties.getOauth2SameSite())
			.build();

		response.addHeader(OAuth2CookieConstants.SET_COOKIE_HEADER, cookie.toString());
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
			.sameSite(securityCookieProperties.getOauth2SameSite())
			.build();

		response.addHeader(OAuth2CookieConstants.SET_COOKIE_HEADER, cookie.toString());
	}
}
