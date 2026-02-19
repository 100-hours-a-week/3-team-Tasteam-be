package com.tasteam.infra.analytics.posthog;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.analytics.posthog")
public class PosthogProperties {

	private boolean enabled = false;
	private String host = "https://app.posthog.com";
	private String apiKey;
	private Duration connectTimeout = Duration.ofSeconds(2);
	private Duration readTimeout = Duration.ofSeconds(3);
}
