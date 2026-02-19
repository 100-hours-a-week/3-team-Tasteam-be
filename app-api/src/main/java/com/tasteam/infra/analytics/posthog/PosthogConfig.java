package com.tasteam.infra.analytics.posthog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(PosthogProperties.class)
public class PosthogConfig {

	@Bean
	@ConditionalOnProperty(prefix = "tasteam.analytics.posthog", name = "enabled", havingValue = "true")
	public RestClient posthogRestClient(PosthogProperties properties) {
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, toConnectTimeoutMillis(properties))
			.responseTimeout(properties.getReadTimeout());
		ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(httpClient);
		return RestClient.builder()
			.baseUrl(properties.getHost())
			.requestFactory(requestFactory)
			.build();
	}

	private int toConnectTimeoutMillis(PosthogProperties properties) {
		long millis = properties.getConnectTimeout() == null ? 2000L : properties.getConnectTimeout().toMillis();
		return (int)Math.max(1L, Math.min(millis, Integer.MAX_VALUE));
	}
}
