package com.tasteam.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class EnvPresenceLogger {

	private static final Logger log = LoggerFactory.getLogger(EnvPresenceLogger.class);

	@Bean
	public ApplicationRunner envPresenceRunner(Environment environment,
		ClientRegistrationRepository clientRegistrationRepository) {
		return args -> {
			logPresence(environment, "GOOGLE_CLIENT_ID");
			logPresence(environment, "GOOGLE_CLIENT_SECRET");
			logPresence(environment, "NAVER_MAPS_API_KEY_ID");
			logPresence(environment, "NAVER_MAPS_API_KEY");
			logClientRegistration(clientRegistrationRepository, "google");
		};
	}

	private void logPresence(Environment environment, String key) {
		String value = environment.getProperty(key);
		int length = value == null ? 0 : value.length();
		log.info("환경변수 {} 존재여부={} 길이={}", key, value != null && !value.isBlank(), length);
	}

	private void logClientRegistration(ClientRegistrationRepository repository, String registrationId) {
		boolean present = repository.findByRegistrationId(registrationId) != null;
		log.info("클라이언트 등록 {} 존재여부={}", registrationId, present);
	}
}
