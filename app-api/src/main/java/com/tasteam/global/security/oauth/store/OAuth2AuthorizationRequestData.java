package com.tasteam.global.security.oauth.store;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public record OAuth2AuthorizationRequestData(
	OAuth2AuthorizationRequest authorizationRequest,
	String redirectUri) {

	public static OAuth2AuthorizationRequestData of(OAuth2AuthorizationRequest request, String redirectUri) {
		return new OAuth2AuthorizationRequestData(request, redirectUri);
	}
}
