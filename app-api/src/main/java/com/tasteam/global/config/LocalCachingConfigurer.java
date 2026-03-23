package com.tasteam.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalCachingConfigurer implements CachingConfigurer {

	@Qualifier("caffeineCacheManager")
	private final CacheManager localCacheManager;

	@Override
	public CacheManager cacheManager() {
		return localCacheManager;
	}
}
