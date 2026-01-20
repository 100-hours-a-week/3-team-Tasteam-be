package com.tasteam.global.security.oauth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfig {

	/**
	 * OAuth2 클라이언트가 등록되지 않은 환경에서도 스프링 시큐리티 시작이 가능하도록
	 * 최소한의 빈을 제공합니다. 실제 프로바이더 등록이 있으면 자동으로 덮어씌워집니다.
	 */
	@Bean
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	public ClientRegistrationRepository clientRegistrationRepository() {
		return registrationId -> null;
	}
}
