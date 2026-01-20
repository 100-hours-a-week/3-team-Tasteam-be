package com.tasteam.global.security.oauth.repository;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.global.security.common.util.RedirectUriValidator;
import com.tasteam.global.security.oauth.common.OAuth2CookieConstants;
import com.tasteam.global.security.oauth.cookie.OAuth2CookieProvider;
import com.tasteam.global.utils.SerializationUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 인증 요청을 HTTP 쿠키에 저장하고 복원하는 리포지토리
 */
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
		implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private final RedirectUriValidator redirectUriValidator;
	private final OAuth2CookieProvider oAuth2CookieProvider;

	/** OAuth2 인증 요청을 쿠키에서 로드합니다. */
	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		return oAuth2CookieProvider.getOAuth2AuthorizationRequestCookie(request)
			.map(value -> SerializationUtil.deserialize(value, OAuth2AuthorizationRequest.class))
			.orElse(null);
	}

	/** OAuth2 인증 요청을 쿠키에 저장합니다. */
	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
			HttpServletResponse response) {
		if (authorizationRequest == null) {
			removeAuthorizationRequestCookies(response);
			return;
		}
		oAuth2CookieProvider.addOAuth2AuthorizationRequestCookie(response,
			SerializationUtil.serialize(authorizationRequest));

		String redirectUriAfterLogin = request.getParameter(OAuth2CookieConstants.REDIRECT_URI_COOKIE_NAME);
		if (StringUtils.hasText(redirectUriAfterLogin)) {
			if (!redirectUriValidator.isValidRedirectUri(redirectUriAfterLogin)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				removeAuthorizationRequestCookies(response);
				return;
			}
			oAuth2CookieProvider.addRedirectUriCookie(response, redirectUriAfterLogin);
		}
	}

	/** OAuth2 인증 요청을 쿠키에서 제거합니다. */
	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
			HttpServletResponse response) {
		return loadAuthorizationRequest(request);
	}

	public void removeAuthorizationRequestCookies(HttpServletResponse response) {
		oAuth2CookieProvider.deleteOAuth2AuthorizationRequestCookie(response);
		oAuth2CookieProvider.deleteRedirectUriCookie(response);
	}
}
