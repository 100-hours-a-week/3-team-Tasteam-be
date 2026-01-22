package com.tasteam.domain.restaurant.geocoding;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "naver.maps")
public class NaverMapsProperties {

	private String baseUrl;
	private String apiKeyId;
	private String apiKey;
}
