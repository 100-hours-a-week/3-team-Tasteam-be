package com.tasteam.infra.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(AiClientProperties.class)
public class AiClientConfig {

	@Bean
	public RestClient aiRestClient(AiClientProperties properties) {
		HttpClient httpClient = HttpClient.create()
			.responseTimeout(properties.getResponseTimeout());
		ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(httpClient);

		return RestClient.builder()
			.baseUrl(properties.getBaseUrl())
			.requestFactory(requestFactory)
			.build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "ai", name = "stub-enabled", havingValue = "false", matchIfMissing = true)
	public AiClient aiClient(RestClient aiRestClient) {
		return new AiClient(aiRestClient);
	}

	@Bean
	@ConditionalOnProperty(prefix = "ai", name = "stub-enabled", havingValue = "true")
	public AiClient fakeAiClient(AiClientProperties properties) {
		return new FakeAiClient(properties);
	}
}
