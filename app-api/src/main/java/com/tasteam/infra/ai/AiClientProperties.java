package com.tasteam.infra.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiClientProperties {

	private String baseUrl;
	private Duration responseTimeout = Duration.ofSeconds(10);
}
