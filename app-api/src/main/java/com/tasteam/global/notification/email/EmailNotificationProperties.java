package com.tasteam.global.notification.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.notification.email")
public class EmailNotificationProperties {

	private String provider = "log";
	private final Ses ses = new Ses();

	@Getter
	@Setter
	public static class Ses {

		@NotBlank
		private String region;
		private String accessKey;
		private String secretKey;
		@NotBlank
		private String fromAddress;
		private String charset = "UTF-8";
	}
}
