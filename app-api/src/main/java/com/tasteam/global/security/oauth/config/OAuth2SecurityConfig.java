package com.tasteam.global.security.oauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import com.tasteam.global.security.oauth.handler.OAuthLoginFailureHandler;
import com.tasteam.global.security.oauth.handler.OAuthLoginSuccessHandler;
import com.tasteam.global.security.oauth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tasteam.global.security.oauth.service.OAuthLoginService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OAuth2SecurityConfig {

	private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
	private final OAuthLoginFailureHandler oAuthLoginFailureHandler;
	private final OAuthLoginService oAuthLoginService;
	private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

	public void configure(HttpSecurity http) throws Exception {
		http.oauth2Login(oauth2 -> oauth2
			.userInfoEndpoint(userInfo -> userInfo.userService(oAuthLoginService))
			.authorizationEndpoint(auth -> auth
				.baseUri("/api/v1/auth/oauth")
				.authorizationRequestRepository(authorizationRequestRepository))
			.redirectionEndpoint(redir -> redir.baseUri("/login/oauth2/code/*"))
			.successHandler(oAuthLoginSuccessHandler)
			.failureHandler(oAuthLoginFailureHandler));
	}
}
