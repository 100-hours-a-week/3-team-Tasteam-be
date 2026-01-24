package com.tasteam.global.security.oauth.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import com.tasteam.global.security.oauth.handler.OAuthLoginFailureHandler;
import com.tasteam.global.security.oauth.handler.OAuthLoginSuccessHandler;
import com.tasteam.global.security.oauth.repository.ServerSideOAuth2AuthorizationRequestRepository;
import com.tasteam.global.security.oauth.service.OAuthLoginService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OAuth2SecurityConfig {

	private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
	private final OAuthLoginFailureHandler oAuthLoginFailureHandler;
	private final OAuthLoginService oAuthLoginService;
	private final ServerSideOAuth2AuthorizationRequestRepository authorizationRequestRepository;
	private final ClientRegistrationRepository clientRegistrationRepository;

	public void configure(HttpSecurity http) throws Exception {
		if (!hasClientRegistration()) {
			return;
		}
		http.oauth2Login(oauth2 -> oauth2
			.loginPage("/api/v1/auth/oauth")
			.userInfoEndpoint(userInfo -> userInfo.userService(oAuthLoginService))
			.authorizationEndpoint(auth -> auth
				.baseUri("/api/v1/auth/oauth")
				.authorizationRequestRepository(authorizationRequestRepository))
			.redirectionEndpoint(redir -> redir.baseUri("/api/v1/auth/oauth/callback/*"))
			.successHandler(oAuthLoginSuccessHandler)
			.failureHandler(oAuthLoginFailureHandler));
	}

	private boolean hasClientRegistration() {
		for (String registrationId : List.of("kakao", "google")) {
			if (clientRegistrationRepository.findByRegistrationId(registrationId) != null) {
				return true;
			}
		}
		return false;
	}
}
