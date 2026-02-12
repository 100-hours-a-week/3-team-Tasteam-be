package com.tasteam.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisProperties {

	private String host;
	private int port = 6379;
	private String password;
	private int database = 0;
	private boolean enabled = true;
}
