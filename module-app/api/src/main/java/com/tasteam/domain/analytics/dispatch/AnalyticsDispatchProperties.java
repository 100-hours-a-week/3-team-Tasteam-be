package com.tasteam.domain.analytics.dispatch;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.analytics.dispatch")
public class AnalyticsDispatchProperties {

	private boolean enabled = true;
	private int batchSize = 100;
	private Duration fixedDelay = Duration.ofMinutes(1);
	private Retry retry = new Retry();
	private Circuit circuit = new Circuit();

	@Getter
	@Setter
	public static class Retry {

		private Duration baseDelay = Duration.ofSeconds(10);
		private Duration maxDelay = Duration.ofMinutes(10);
	}

	@Getter
	@Setter
	public static class Circuit {

		private int failureThreshold = 5;
		private Duration openDuration = Duration.ofMinutes(1);
	}
}
