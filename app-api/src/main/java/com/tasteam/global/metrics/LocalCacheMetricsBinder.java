package com.tasteam.global.metrics;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

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
	private static final String METRIC_CAPACITY = "tasteam.cache.capacity";
	private static final String METRIC_UTILIZATION = "tasteam.cache.utilization.ratio";
	private static final String METRIC_REQUESTS = "tasteam.cache.requests";
	private static final String METRIC_EVICTIONS = "tasteam.cache.evictions";

	@Qualifier("caffeineCacheManager")
	private final CacheManager cacheManager;

	private final LocalCacheProperties localCacheProperties;
	private final Map<String, CacheMetricsProbe> probes = new ConcurrentHashMap<>();

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

		CacheMetricsProbe probe = probes.computeIfAbsent(cacheName,
			ignored -> new CacheMetricsProbe(cacheManager, cacheName));
		Tags baseTags = metadata.baseTags();

		validate(METRIC_TTL, baseTags);
		Gauge.builder(METRIC_TTL, metadata, CacheMetadata::ttlSeconds)
			.description("API별 로컬 캐시 TTL")
			.baseUnit("seconds")
			.tags(baseTags)
			.strongReference(true)
			.register(registry);

		validate(METRIC_SIZE, baseTags);
		Gauge.builder(METRIC_SIZE, probe, CacheMetricsProbe::estimatedSize)
			.description("API별 로컬 캐시 예상 엔트리 수")
			.tags(baseTags)
			.strongReference(true)
			.register(registry);

		validate(METRIC_CAPACITY, baseTags);
		Gauge.builder(METRIC_CAPACITY, metadata, ignored -> metadata.capacity())
			.description("API별 로컬 캐시 최대 엔트리 수")
			.tags(baseTags)
			.strongReference(true)
			.register(registry);

		validate(METRIC_UTILIZATION, baseTags);
		Gauge.builder(METRIC_UTILIZATION, probe, ignored -> probe.utilizationRatio(metadata.capacity()))
			.description("API별 로컬 캐시 엔트리 점유율")
			.tags(baseTags)
			.strongReference(true)
			.register(registry);

		registerRequestCounter(registry, baseTags, probe, "hit", CacheMetricsProbe::hitCount);
		registerRequestCounter(registry, baseTags, probe, "miss", CacheMetricsProbe::missCount);

		validate(METRIC_EVICTIONS, baseTags);
		FunctionCounter.builder(METRIC_EVICTIONS, probe, CacheMetricsProbe::evictionCount)
			.description("API별 로컬 캐시 eviction 누적 수")
			.tags(baseTags)
			.register(registry);
	}

	private void registerRequestCounter(
		MeterRegistry registry,
		Tags baseTags,
		CacheMetricsProbe probe,
		String result,
		ToDoubleFunction<CacheMetricsProbe> extractor) {
		Tags tags = baseTags.and("result", result);
		validate(METRIC_REQUESTS, tags);
		FunctionCounter.builder(METRIC_REQUESTS, probe, extractor)
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
		long capacity = localCacheProperties.getCaffeine().getMaximumSize();

		return switch (cacheName) {
			case "presigned-url" -> new CacheMetadata(cacheName, "file", "GET", "/api/v1/files/{fileUuid}/url", ttl,
				capacity);
			case "reverse-geocode" -> new CacheMetadata(cacheName, "location", "GET", "/api/v1/geocode/reverse",
				ttl, capacity);
			case "main-section-hot-all" -> new CacheMetadata(cacheName, "main", "GET",
				"/api/v1/main,/api/v1/main/home", ttl, capacity);
			case "main-section-new-all" -> new CacheMetadata(cacheName, "main", "GET",
				"/api/v1/main,/api/v1/main/home", ttl, capacity);
			case "main-section-ai-all" -> new CacheMetadata(cacheName, "main", "GET",
				"/api/v1/main,/api/v1/main/ai-recommend", ttl, capacity);
			case "main-banners" -> new CacheMetadata(cacheName, "main", "GET", "/api/v1/main", ttl, capacity);
			default -> new CacheMetadata(cacheName, "unknown", "UNKNOWN", "unmapped", ttl, capacity);
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
		Duration ttl,
		long capacity) {

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

	private record CacheMetricsProbe(
		CacheManager cacheManager,
		String cacheName) {

		private double estimatedSize() {
			return nativeCache().map(cache -> (double)cache.estimatedSize()).orElse(0.0);
		}

		private double hitCount() {
			return nativeCache().map(cache -> (double)cache.stats().hitCount()).orElse(0.0);
		}

		private double missCount() {
			return nativeCache().map(cache -> (double)cache.stats().missCount()).orElse(0.0);
		}

		private double evictionCount() {
			return nativeCache().map(cache -> (double)cache.stats().evictionCount()).orElse(0.0);
		}

		private double utilizationRatio(double capacity) {
			if (capacity <= 0) {
				return 0.0;
			}
			return estimatedSize() / capacity;
		}

		private java.util.Optional<com.github.benmanes.caffeine.cache.Cache<?, ?>> nativeCache() {
			Cache cache = cacheManager.getCache(cacheName);
			if (cache instanceof CaffeineCache caffeineCache) {
				return java.util.Optional.of(caffeineCache.getNativeCache());
			}
			return java.util.Optional.empty();
		}
	}

}
