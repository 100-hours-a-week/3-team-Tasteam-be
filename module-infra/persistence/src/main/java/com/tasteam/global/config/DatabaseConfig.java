package com.tasteam.global.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.tasteam")
@EnableJpaRepositories(basePackages = "com.tasteam")
public class DatabaseConfig {
	// Explicit scan to ensure JPA entities/repositories are picked up in all profiles.
}
