package com.tasteam.domain.analytics.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventItemRequest;
import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AnalyticsErrorCode;

@UnitTest
@DisplayName("클라이언트 활동 이벤트 수집 서비스")
class ClientActivityIngestServiceTest {

	@Test
	@DisplayName("인증 사용자 요청이면 허용 이벤트를 동일 저장 경로로 저장한다")
	void ingest_storesAllowedEventsForAuthenticatedMember() {
		// given
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		when(rateLimiter.tryAcquire("member:10")).thenReturn(true);
		ClientActivityIngestService service = new ClientActivityIngestService(properties, rateLimiter, storeService);

		ClientActivityEventItemRequest first = eventItem("evt-1", "ui.restaurant.viewed", null);
		ClientActivityEventItemRequest second = eventItem("evt-2", "ui.review.submitted", "v3");

		// when
		int accepted = service.ingest(10L, null, List.of(first, second));

		// then
		assertThat(accepted).isEqualTo(2);
		verify(rateLimiter).tryAcquire("member:10");
		ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
		verify(storeService, org.mockito.Mockito.times(2)).store(captor.capture());
		ActivityEvent firstStoredEvent = captor.getAllValues().get(0);
		assertThat(firstStoredEvent.eventId()).isEqualTo("evt-1");
		assertThat(firstStoredEvent.eventVersion()).isEqualTo("v1");
		assertThat(firstStoredEvent.memberId()).isEqualTo(10L);
		assertThat(firstStoredEvent.anonymousId()).isNull();
		assertThat(firstStoredEvent.properties()).containsEntry("source", "CLIENT");
	}

	@Test
	@DisplayName("익명 요청에서 anonymousId가 없으면 수집을 거부한다")
	void ingest_rejectsAnonymousRequestWhenAnonymousIdMissing() {
		// given
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ClientActivityIngestService service = new ClientActivityIngestService(properties, rateLimiter, storeService);
		ClientActivityEventItemRequest item = eventItem("evt-1", "ui.restaurant.viewed", null);

		// when & then
		assertThatThrownBy(() -> service.ingest(null, "   ", List.of(item)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_ANONYMOUS_ID_REQUIRED.name());
		verifyNoInteractions(rateLimiter, storeService);
	}

	@Test
	@DisplayName("allowlist에 없는 이벤트가 포함되면 수집을 거부한다")
	void ingest_rejectsEventWhenNotAllowlisted() {
		// given
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		when(rateLimiter.tryAcquire("anonymous:anon-1")).thenReturn(true);
		ClientActivityIngestService service = new ClientActivityIngestService(properties, rateLimiter, storeService);
		ClientActivityEventItemRequest item = eventItem("evt-1", "ui.unknown.event", null);

		// when & then
		assertThatThrownBy(() -> service.ingest(null, "anon-1", List.of(item)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_EVENT_NOT_ALLOWED.name());
		verify(rateLimiter).tryAcquire("anonymous:anon-1");
		verifyNoInteractions(storeService);
	}

	@Test
	@DisplayName("배치 크기가 제한을 초과하면 수집을 거부한다")
	void ingest_rejectsWhenBatchSizeExceeded() {
		// given
		AnalyticsIngestProperties properties = defaultProperties();
		properties.setMaxBatchSize(1);
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ClientActivityIngestService service = new ClientActivityIngestService(properties, rateLimiter, storeService);

		// when & then
		assertThatThrownBy(() -> service.ingest(10L, null, List.of(
			eventItem("evt-1", "ui.restaurant.viewed", null),
			eventItem("evt-2", "ui.review.submitted", null))))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_BATCH_LIMIT_EXCEEDED.name());
		verifyNoInteractions(rateLimiter, storeService);
	}

	@Test
	@DisplayName("요청 빈도가 제한을 초과하면 수집을 거부한다")
	void ingest_rejectsWhenRateLimitExceeded() {
		// given
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		when(rateLimiter.tryAcquire("anonymous:anon-1")).thenReturn(false);
		ClientActivityIngestService service = new ClientActivityIngestService(properties, rateLimiter, storeService);
		ClientActivityEventItemRequest item = eventItem("evt-1", "ui.restaurant.viewed", null);

		// when & then
		assertThatThrownBy(() -> service.ingest(null, "anon-1", List.of(item)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_RATE_LIMIT_EXCEEDED.name());
		verifyNoInteractions(storeService);
	}

	@Test
	@DisplayName("properties에 null 값이 포함되어도 null-safe 정규화 후 저장한다")
	void ingest_normalizesNullPropertyEntries() {
		// given
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		when(rateLimiter.tryAcquire("anonymous:anon-1")).thenReturn(true);
		ClientActivityIngestService service = new ClientActivityIngestService(properties, rateLimiter, storeService);

		Map<String, Object> rawProperties = new LinkedHashMap<>();
		rawProperties.put("restaurantId", 1L);
		rawProperties.put("subgroupId", null);
		rawProperties.put("", "blank-key");
		ClientActivityEventItemRequest item = new ClientActivityEventItemRequest(
			"evt-1",
			"ui.restaurant.viewed",
			null,
			Instant.parse("2026-02-19T00:00:00Z"),
			rawProperties);

		// when
		int accepted = service.ingest(null, "anon-1", List.of(item));

		// then
		assertThat(accepted).isEqualTo(1);
		ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
		verify(storeService).store(captor.capture());
		Map<String, Object> storedProperties = captor.getValue().properties();
		assertThat(storedProperties).containsEntry("restaurantId", 1L);
		assertThat(storedProperties).containsEntry("source", "CLIENT");
		assertThat(storedProperties).doesNotContainKey("subgroupId");
		assertThat(storedProperties).doesNotContainKey("");
	}

	private AnalyticsIngestProperties defaultProperties() {
		AnalyticsIngestProperties properties = new AnalyticsIngestProperties();
		properties.setMaxBatchSize(50);
		properties.setAllowlist(List.of("ui.restaurant.viewed", "ui.review.submitted"));
		return properties;
	}

	private ClientActivityEventItemRequest eventItem(String eventId, String eventName, String eventVersion) {
		return new ClientActivityEventItemRequest(
			eventId,
			eventName,
			eventVersion,
			Instant.parse("2026-02-19T00:00:00Z"),
			Map.of("restaurantId", 1L));
	}
}
