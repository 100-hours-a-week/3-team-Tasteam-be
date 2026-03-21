package com.tasteam.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
@EnableConfigurationProperties(LocalCacheProperties.class)
public class LocalCacheConfig {

	private final LocalCacheProperties localCacheProperties;

	@Primary
	@Bean
	public CacheManager caffeineCacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();

		LocalCacheProperties.Caffeine caffeineConfig = localCacheProperties.getCaffeine();

		cacheManager.setCaffeine(Caffeine.newBuilder()
			.maximumSize(caffeineConfig.getMaximumSize())
			.expireAfterWrite(caffeineConfig.getExpireAfterWrite())
			.recordStats());

		localCacheProperties.getTtl().forEach((cacheName, cacheTtl) -> {
			if (cacheTtl.getTtl() != null) {
				long size = cacheTtl.getMaximumSize() != null
					? cacheTtl.getMaximumSize()
					: caffeineConfig.getMaximumSize();

				Caffeine<Object, Object> builder = Caffeine.newBuilder()
					.maximumSize(size)
					.recordStats();

				if (cacheTtl.getJitter() != null && !cacheTtl.getJitter().isZero()) {
					builder.expireAfter(new JitterExpiry(
						cacheTtl.getTtl().toNanos(), cacheTtl.getJitter().toNanos()));
				} else {
					builder.expireAfterWrite(cacheTtl.getTtl());
				}

				cacheManager.registerCustomCache(cacheName, builder.build());
				log.info("Registered custom cache '{}' TTL={} jitter={} maxSize={}",
					cacheName, cacheTtl.getTtl(), cacheTtl.getJitter(), size);
			}
		});

		log.info("=== Local Cache (Caffeine) Configuration ===");
		log.info("Maximum Size: {}", caffeineConfig.getMaximumSize());
		log.info("Expire After Write: {}", caffeineConfig.getExpireAfterWrite());
		log.info("Stats Recording: {}", caffeineConfig.isRecordStats());
		log.info("============================================");

		return cacheManager;
	}
}
