package com.tasteam.global.ratelimit;

import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.notification.email.rate-limit")
public class RateLimitPolicy {
	private boolean enabled = true;
	private String prefix = "rl:mail";
	private String action = "send";
	private String timezone = "Asia/Seoul";
	private List<String> trustedProxies = List.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");
	private FixedWindow email1m = new FixedWindow(1, 60);
	private FixedWindow ip1m = new FixedWindow(5, 60);
	private FixedWindow user1m = new FixedWindow(5, 60);
	private FixedWindow email1d = new FixedWindow(10, 86400);
	private int blockTtlSeconds = 86400;

	public ZoneId zoneId() {
		try {
			return ZoneId.of(timezone);
		} catch (Exception ignored) {
			return ZoneId.of("Asia/Seoul");
		}
	}

	public int blockTtlSeconds() {
		return Math.max(1, blockTtlSeconds);
	}

	@Getter
	@Setter
	public static class FixedWindow {

		private int limit;
		private int ttlSeconds;

		public FixedWindow() {}

		public FixedWindow(int limit, int ttlSeconds) {
			this.limit = limit;
			this.ttlSeconds = ttlSeconds;
		}

		public int validatedLimit() {
			return Math.max(1, limit);
		}

		public int validatedTtlSeconds() {
			return Math.max(1, ttlSeconds);
		}
	}
}
