package com.tasteam.infra.geocode.naver;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "naver.maps")
public class NaverMapsProperties {

	private String baseUrl;
	private String apiKeyId;
	private String apiKey;
}
