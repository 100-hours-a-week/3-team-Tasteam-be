package com.tasteam.global.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.util.StringUtils;

import com.tasteam.global.security.common.constants.ApiEndpoints;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminCredentialProperties.class)
public class SwaggerSecurityConfig {

	private final AdminCredentialProperties adminCredentialProperties;

	public SwaggerSecurityConfig(AdminCredentialProperties adminCredentialProperties) {
		this.adminCredentialProperties = adminCredentialProperties;
	}

	@Bean
	@Order(0)
	public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.securityMatcher(
				ApiEndpoints.SWAGGER_UI,
				ApiEndpoints.SWAGGER_UI_INDEX,
				ApiEndpoints.SWAGGER_UI_ASSETS,
				ApiEndpoints.SWAGGER_RESOURCES,
				ApiEndpoints.API_DOCS,
				ApiEndpoints.WEBJARS)
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.requestCache(cache -> cache
				.requestCache(new NullRequestCache()))
			.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults())
			.formLogin(AbstractHttpConfigurer::disable);

		return http.build();
	}

	@Bean
	public UserDetailsService swaggerUserDetailsService() {
		if (!StringUtils.hasText(adminCredentialProperties.username())
			|| !StringUtils.hasText(adminCredentialProperties.password())) {
			return new InMemoryUserDetailsManager();
		}

		return new InMemoryUserDetailsManager(
			User.withUsername(adminCredentialProperties.username())
				.password("{noop}" + adminCredentialProperties.password())
				.roles("SWAGGER")
				.build());
	}
}
