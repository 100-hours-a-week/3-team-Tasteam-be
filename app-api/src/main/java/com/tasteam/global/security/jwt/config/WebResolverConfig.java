package com.tasteam.global.security.jwt.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tasteam.global.security.jwt.resolver.CurrentUserArgumentResolver;
import com.tasteam.global.security.jwt.resolver.RefreshTokenArgumentResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebResolverConfig implements WebMvcConfigurer {

	private final CurrentUserArgumentResolver currentUserArgumentResolver;
	private final RefreshTokenArgumentResolver refreshTokenArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserArgumentResolver);
		resolvers.add(refreshTokenArgumentResolver);
	}
}
