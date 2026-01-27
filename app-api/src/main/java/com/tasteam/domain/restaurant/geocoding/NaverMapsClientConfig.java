package com.tasteam.domain.restaurant.geocoding;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class NaverMapsClientConfig {

	private final NaverMapsProperties properties;

	@Bean
	public RestClient naverMapsRestClient() {
		return RestClient.builder()
			.baseUrl(properties.getBaseUrl())
			.defaultHeader("Accept", "application/json")
			.defaultHeader("X-NCP-APIGW-API-KEY-ID", properties.getApiKeyId())
			.defaultHeader("X-NCP-APIGW-API-KEY", properties.getApiKey())
			.build();
	}
}
