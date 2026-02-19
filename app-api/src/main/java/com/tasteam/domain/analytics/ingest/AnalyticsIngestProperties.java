package com.tasteam.domain.analytics.ingest;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.analytics.ingest")
public class AnalyticsIngestProperties {

	private boolean enabled = true;
	private int maxBatchSize = 50;
	private List<String> allowlist = List.of(
		"ui.restaurant.viewed",
		"ui.restaurant.clicked",
		"ui.review.write_started",
		"ui.review.submitted");
	private RateLimit rateLimit = new RateLimit();

	public int validatedMaxBatchSize() {
		return Math.max(1, maxBatchSize);
	}

	public Set<String> allowedEventNames() {
		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String eventName : allowlist) {
			if (StringUtils.hasText(eventName)) {
				normalized.add(eventName.trim());
			}
		}
		return Set.copyOf(normalized);
	}

	@Getter
	@Setter
	public static class RateLimit {

		private int maxRequests = 120;
		private Duration window = Duration.ofMinutes(1);

		public int validatedMaxRequests() {
			return Math.max(1, maxRequests);
		}

		public Duration validatedWindow() {
			if (window == null || window.isNegative() || window.isZero()) {
				return Duration.ofMinutes(1);
			}
			return window;
		}
	}
}
