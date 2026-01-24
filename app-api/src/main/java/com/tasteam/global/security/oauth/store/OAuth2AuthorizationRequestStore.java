package com.tasteam.global.security.oauth.store;

import java.util.Optional;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public interface OAuth2AuthorizationRequestStore {

	void save(String state, OAuth2AuthorizationRequest request, String redirectUri);

	Optional<OAuth2AuthorizationRequest> findByState(String state);

	Optional<String> findRedirectUriByState(String state);

	void removeByState(String state);
}
