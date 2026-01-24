package com.tasteam.global.security.oauth.store;

import java.time.Duration;
import java.util.Optional;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tasteam.global.security.oauth.common.OAuth2Constants;

import jakarta.annotation.PostConstruct;

@Component
public class CaffeineOAuth2AuthorizationRequestStore implements OAuth2AuthorizationRequestStore {

	private Cache<String, OAuth2AuthorizationRequestData> cache;

	@PostConstruct
	void init() {
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofSeconds(OAuth2Constants.REQUEST_TTL_SECONDS))
			.maximumSize(OAuth2Constants.MAX_STORE_SIZE)
			.build();
	}

	@Override
	public void save(String state, OAuth2AuthorizationRequest request, String redirectUri) {
		cache.put(state, OAuth2AuthorizationRequestData.of(request, redirectUri));
	}

	@Override
	public Optional<OAuth2AuthorizationRequest> findByState(String state) {
		OAuth2AuthorizationRequestData data = cache.getIfPresent(state);
		return Optional.ofNullable(data).map(OAuth2AuthorizationRequestData::authorizationRequest);
	}

	@Override
	public Optional<String> findRedirectUriByState(String state) {
		OAuth2AuthorizationRequestData data = cache.getIfPresent(state);
		return Optional.ofNullable(data).map(OAuth2AuthorizationRequestData::redirectUri);
	}

	@Override
	public void removeByState(String state) {
		cache.invalidate(state);
	}
}
