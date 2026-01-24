package com.tasteam.global.security.oauth.repository;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.global.security.common.util.RedirectUriValidator;
import com.tasteam.global.security.oauth.common.OAuth2Constants;
import com.tasteam.global.security.oauth.store.OAuth2AuthorizationRequestStore;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ServerSideOAuth2AuthorizationRequestRepository
	implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private final OAuth2AuthorizationRequestStore store;
	private final RedirectUriValidator redirectUriValidator;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		String state = request.getParameter("state");
		if (!StringUtils.hasText(state)) {
			return null;
		}
		return store.findByState(state).orElse(null);
	}

	@Override
	public void saveAuthorizationRequest(
		OAuth2AuthorizationRequest authorizationRequest,
		HttpServletRequest request,
		HttpServletResponse response) {
		if (authorizationRequest == null) {
			return;
		}

		String state = authorizationRequest.getState();
		String redirectUri = request.getParameter(OAuth2Constants.REDIRECT_URI_PARAM);

		if (StringUtils.hasText(redirectUri) && !redirectUriValidator.isValidRedirectUri(redirectUri)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		store.save(state, authorizationRequest, redirectUri);
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(
		HttpServletRequest request,
		HttpServletResponse response) {
		String state = request.getParameter("state");
		if (!StringUtils.hasText(state)) {
			return null;
		}
		OAuth2AuthorizationRequest authRequest = store.findByState(state).orElse(null);
		store.removeByState(state);
		return authRequest;
	}

	public String getRedirectUri(String state) {
		return store.findRedirectUriByState(state).orElse(null);
	}

	public void removeByState(String state) {
		store.removeByState(state);
	}
}
