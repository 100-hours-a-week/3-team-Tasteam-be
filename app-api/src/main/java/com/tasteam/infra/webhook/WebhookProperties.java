package com.tasteam.infra.webhook;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.webhook")
public class WebhookProperties {

	private boolean enabled = false;
	private String provider = "discord";
	private DiscordProperties discord = new DiscordProperties();
	private SlackProperties slack = new SlackProperties();
	private RetryProperties retry = new RetryProperties();
	private FilterProperties filters = new FilterProperties();
	private boolean includeStackTrace = false;

	@Getter
	@Setter
	public static class DiscordProperties {
		private String url;
		private String batchReportUrl;
	}

	@Getter
	@Setter
	public static class SlackProperties {
		private String url;
	}

	@Getter
	@Setter
	public static class RetryProperties {
		private int maxAttempts = 3;
		private long backoffMs = 1000;
	}

	@Getter
	@Setter
	public static class FilterProperties {
		private int minHttpStatus = 500;
		private List<String> excludeErrorCodes = new ArrayList<>();
	}
}
