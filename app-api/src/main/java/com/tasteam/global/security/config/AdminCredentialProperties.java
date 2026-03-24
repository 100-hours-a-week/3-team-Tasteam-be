package com.tasteam.global.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tasteam.admin")
public record AdminCredentialProperties(
	String username,
	String password) {
}
