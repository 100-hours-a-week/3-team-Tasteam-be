package com.tasteam.global.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.local-cache")
public class LocalCacheProperties {

	private Caffeine caffeine = new Caffeine();
	private Map<String, CacheTtl> ttl = new HashMap<>();

	@Getter
	@Setter
	public static class Caffeine {
		private long maximumSize = 1000;
		private Duration expireAfterWrite = Duration.ofMinutes(10);
		private boolean recordStats = true;
	}

	@Getter
	@Setter
	public static class CacheTtl {
		private Duration ttl;
	}
}
