package com.tasteam.domain.file.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.file.cleanup")
public class FileCleanupProperties {

	private long ttlSeconds = 86400;

	public Duration ttlDuration() {
		return Duration.ofSeconds(ttlSeconds);
	}
}
