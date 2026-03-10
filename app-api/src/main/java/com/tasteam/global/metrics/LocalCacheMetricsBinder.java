package com.tasteam.global.metrics;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tasteam.global.config.LocalCacheProperties;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalCacheMetricsBinder implements MeterBinder {

	private static final String METRIC_TTL = "tasteam.cache.ttl.seconds";
	private static final String METRIC_SIZE = "tasteam.cache.size";
	private static final String METRIC_REQUESTS = "tasteam.cache.requests";
	private static final String METRIC_EVICTIONS = "tasteam.cache.evictions";

	@Qualifier("caffeineCacheManager")
	private final CacheManager cacheManager;

	private final LocalCacheProperties localCacheProperties;

	@Override
	public void bindTo(MeterRegistry registry) {
		resolveCacheMetrics().forEach((cacheName, metadata) -> registerMetrics(registry, cacheName, metadata));
	}

	private void registerMetrics(MeterRegistry registry, String cacheName, CacheMetadata metadata) {
		Cache cache = cacheManager.getCache(cacheName);
		if (!(cache instanceof CaffeineCache caffeineCache)) {
			log.warn("Caffeine 캐시 메트릭 등록을 건너뜁니다. cache={}", cacheName);
			return;
		}

		var nativeCache = caffeineCache.getNativeCache();
		Tags baseTags = metadata.baseTags();

		validate(METRIC_TTL, baseTags);
		Gauge.builder(METRIC_TTL, metadata, CacheMetadata::ttlSeconds)
			.description("API별 로컬 캐시 TTL")
			.baseUnit("seconds")
			.tags(baseTags)
			.register(registry);

		validate(METRIC_SIZE, baseTags);
		Gauge.builder(METRIC_SIZE, nativeCache, cacheRef -> cacheRef.estimatedSize())
			.description("API별 로컬 캐시 예상 엔트리 수")
			.tags(baseTags)
			.register(registry);

		registerRequestCounter(registry, baseTags, nativeCache::stats, "hit", CacheStats::hitCount);
		registerRequestCounter(registry, baseTags, nativeCache::stats, "miss", CacheStats::missCount);

		validate(METRIC_EVICTIONS, baseTags);
		FunctionCounter.builder(METRIC_EVICTIONS, nativeCache, cacheRef -> cacheRef.stats().evictionCount())
			.description("API별 로컬 캐시 eviction 누적 수")
			.tags(baseTags)
			.register(registry);
	}

	private void registerRequestCounter(
		MeterRegistry registry,
		Tags baseTags,
		StatsSupplier statsSupplier,
		String result,
		StatsExtractor extractor) {
		Tags tags = baseTags.and("result", result);
		validate(METRIC_REQUESTS, tags);
		FunctionCounter.builder(METRIC_REQUESTS, statsSupplier, supplier -> extractor.extract(supplier.get()))
			.description("API별 로컬 캐시 hit/miss 누적 요청 수")
			.tags(tags)
			.register(registry);
	}

	private Map<String, CacheMetadata> resolveCacheMetrics() {
		Map<String, CacheMetadata> caches = new LinkedHashMap<>();
		Set<String> cacheNames = new java.util.LinkedHashSet<>(cacheManager.getCacheNames());
		cacheNames.addAll(localCacheProperties.getTtl().keySet());

		cacheNames.forEach(cacheName -> caches.put(cacheName, resolveMetadata(cacheName)));
		return caches;
	}

	private CacheMetadata resolveMetadata(String cacheName) {
		Duration ttl = resolveTtl(cacheName);

		return switch (cacheName) {
			case "presigned-url" -> new CacheMetadata(cacheName, "file", "GET", "/api/v1/files/{fileUuid}/url", ttl);
			case "main-section-hot-all" -> new CacheMetadata(cacheName, "main", "GET",
				"/api/v1/main,/api/v1/main/home", ttl);
			case "main-section-new-all" -> new CacheMetadata(cacheName, "main", "GET",
				"/api/v1/main,/api/v1/main/home", ttl);
			case "main-section-ai-all" -> new CacheMetadata(cacheName, "main", "GET",
				"/api/v1/main,/api/v1/main/ai-recommend", ttl);
			case "main-banners" -> new CacheMetadata(cacheName, "main", "GET", "/api/v1/main", ttl);
			default -> new CacheMetadata(cacheName, "unknown", "UNKNOWN", "unmapped", ttl);
		};
	}

	private Duration resolveTtl(String cacheName) {
		LocalCacheProperties.CacheTtl cacheTtl = localCacheProperties.getTtl().get(cacheName);
		if (cacheTtl != null && cacheTtl.getTtl() != null) {
			return cacheTtl.getTtl();
		}
		return localCacheProperties.getCaffeine().getExpireAfterWrite();
	}

	private void validate(String metricName, Tags tags) {
		MetricLabelPolicy.validate(metricName, toTagArray(tags));
	}

	private String[] toTagArray(Tags tags) {
		return tags.stream()
			.flatMap(tag -> java.util.stream.Stream.of(tag.getKey(), tag.getValue()))
			.toArray(String[]::new);
	}

	private record CacheMetadata(
		String cacheName,
		String domain,
		String method,
		String uri,
		Duration ttl) {

		private Tags baseTags() {
			return Tags.of(
				"cache", cacheName,
				"domain", domain,
				"method", method,
				"uri", uri);
		}

		private double ttlSeconds() {
			return ttl.toSeconds();
		}
	}

	@FunctionalInterface
	private interface StatsSupplier {
		CacheStats get();
	}

	@FunctionalInterface
	private interface StatsExtractor {
		double extract(CacheStats cacheStats);
	}
}
