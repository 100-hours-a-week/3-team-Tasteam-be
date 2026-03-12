package com.tasteam.global.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.global.config.LocalCacheProperties;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](Metrics) LocalCacheMetricsBinder 단위 테스트")
class LocalCacheMetricsBinderTest {

	@Test
	@DisplayName("reverse-geocode 캐시는 location 도메인 태그로 메트릭을 등록한다")
	void bindTo_registersReverseGeocodeMetricsWithLocationTags() throws Exception {
		// Given
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.registerCustomCache("reverse-geocode",
			Caffeine.newBuilder()
				.maximumSize(100)
				.expireAfterWrite(Duration.ofHours(24))
				.recordStats()
				.build());

		LocalCacheProperties properties = new LocalCacheProperties();
		LocalCacheProperties.CacheTtl cacheTtl = new LocalCacheProperties.CacheTtl();
		cacheTtl.setTtl(Duration.ofHours(24));
		properties.getTtl().put("reverse-geocode", cacheTtl);

		Cache cache = cacheManager.getCache("reverse-geocode");
		assertThat(cache).isNotNull();
		cache.get("37.1_127.1", () -> "gangnam");
		cache.get("37.1_127.1", () -> "gangnam");

		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		LocalCacheMetricsBinder binder = new LocalCacheMetricsBinder(cacheManager, properties);

		// When
		binder.bindTo(registry);

		// Then
		FunctionCounter hitCounter = registry.find("tasteam.cache.requests")
			.tags("cache", "reverse-geocode", "domain", "location", "method", "GET", "uri",
				"/api/v1/geocode/reverse", "result", "hit")
			.functionCounter();
		FunctionCounter missCounter = registry.find("tasteam.cache.requests")
			.tags("cache", "reverse-geocode", "domain", "location", "method", "GET", "uri",
				"/api/v1/geocode/reverse", "result", "miss")
			.functionCounter();
		Gauge ttlGauge = registry.find("tasteam.cache.ttl.seconds")
			.tags("cache", "reverse-geocode", "domain", "location", "method", "GET", "uri",
				"/api/v1/geocode/reverse")
			.gauge();
		Gauge sizeGauge = registry.find("tasteam.cache.size")
			.tags("cache", "reverse-geocode", "domain", "location", "method", "GET", "uri",
				"/api/v1/geocode/reverse")
			.gauge();

		assertThat(hitCounter).isNotNull();
		assertThat(missCounter).isNotNull();
		assertThat(ttlGauge).isNotNull();
		assertThat(sizeGauge).isNotNull();
		assertThat(hitCounter.count()).isEqualTo(1.0);
		assertThat(missCounter.count()).isEqualTo(1.0);
		assertThat(ttlGauge.value()).isEqualTo(Duration.ofHours(24).toSeconds());
		assertThat(sizeGauge.value()).isEqualTo(1.0);
	}
}
