package com.tasteam.infra.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.notification.email")
public class EmailNotificationProperties {

	private String provider = "log";
	private final Ses ses = new Ses();

	@PostConstruct
	public void logConfiguration() {
		if (!"ses".equalsIgnoreCase(provider)) {
			log.info("[Email] provider={}. SES 비활성화.", provider);
			return;
		}
		log.info("=== Email Configuration ===");
		log.info("provider     : {}", provider);
		log.info("region       : {}", ses.region);
		log.info("from-address : {}", ses.fromAddress);
		log.info("accessKey    : {}", masked(ses.accessKey));
		log.info("secretKey    : {}", StringUtils.hasText(ses.secretKey) ? "****" : "(not set - DefaultChain 사용)");
		log.info("===========================");
	}

	private String masked(String key) {
		if (!StringUtils.hasText(key)) {
			return "(not set - DefaultChain 사용)";
		}
		if (key.length() <= 4) {
			return "****";
		}
		return key.substring(0, 4) + "****";
	}

	@Getter
	@Setter
	public static class Ses {

		private String region;
		private String accessKey;
		private String secretKey;
		private String fromAddress;
		private String charset = "UTF-8";
	}
}
