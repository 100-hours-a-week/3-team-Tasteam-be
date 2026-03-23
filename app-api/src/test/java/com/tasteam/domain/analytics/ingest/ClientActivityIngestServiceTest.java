package com.tasteam.domain.analytics.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventItemRequest;
import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AnalyticsErrorCode;
import com.tasteam.infra.messagequeue.UserActivityS3SinkPublisher;

@UnitTest
@DisplayName("[유닛](Client) ClientActivityIngestService 단위 테스트")
class ClientActivityIngestServiceTest {

	private static final Executor DIRECT_EXECUTOR = Runnable::run;

	@Test
	@DisplayName("인증 사용자 요청이면 허용 이벤트를 동일 저장 경로로 저장한다")
	void ingest_storesAllowedEventsForAuthenticatedMember() {
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		when(rateLimiter.tryAcquire("member:10")).thenReturn(true);
		ClientActivityIngestService service = new ClientActivityIngestService(
			properties,
			rateLimiter,
			storeService,
			publisherProvider,
			DIRECT_EXECUTOR);

		ClientActivityEventItemRequest first = eventItem("evt-1", "ui.restaurant.viewed", null);
		ClientActivityEventItemRequest second = eventItem("evt-2", "ui.group.viewed", "v3");

		int accepted = service.ingest(10L, null, List.of(first, second));

		assertThat(accepted).isEqualTo(2);
		verify(rateLimiter).tryAcquire("member:10");
		ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
		verify(storeService, times(2)).store(captor.capture());
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
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		ClientActivityIngestService service = new ClientActivityIngestService(
			properties,
			rateLimiter,
			storeService,
			publisherProvider,
			DIRECT_EXECUTOR);
		ClientActivityEventItemRequest item = eventItem("evt-1", "ui.restaurant.viewed", null);

		assertThatThrownBy(() -> service.ingest(null, "   ", List.of(item)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_ANONYMOUS_ID_REQUIRED.name());
		verifyNoInteractions(rateLimiter, storeService, publisherProvider);
	}

	@Test
	@DisplayName("allowlist에 없는 이벤트가 포함되면 수집을 거부한다")
	void ingest_rejectsEventWhenNotAllowlisted() {
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		when(rateLimiter.tryAcquire("anonymous:anon-1")).thenReturn(true);
		ClientActivityIngestService service = new ClientActivityIngestService(
			properties,
			rateLimiter,
			storeService,
			publisherProvider,
			DIRECT_EXECUTOR);
		ClientActivityEventItemRequest item = eventItem("evt-1", "ui.unknown.event", null);

		assertThatThrownBy(() -> service.ingest(null, "anon-1", List.of(item)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_EVENT_NOT_ALLOWED.name());
		verify(rateLimiter).tryAcquire("anonymous:anon-1");
		verifyNoInteractions(storeService, publisherProvider);
	}

	@Test
	@DisplayName("배치 크기가 제한을 초과하면 수집을 거부한다")
	void ingest_rejectsWhenBatchSizeExceeded() {
		AnalyticsIngestProperties properties = defaultProperties();
		properties.setMaxBatchSize(1);
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		ClientActivityIngestService service = new ClientActivityIngestService(
			properties,
			rateLimiter,
			storeService,
			publisherProvider,
			DIRECT_EXECUTOR);

		assertThatThrownBy(() -> service.ingest(10L, null, List.of(
			eventItem("evt-1", "ui.restaurant.viewed", null),
			eventItem("evt-2", "ui.review.submitted", null))))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_BATCH_LIMIT_EXCEEDED.name());
		verifyNoInteractions(rateLimiter, storeService, publisherProvider);
	}

	@Test
	@DisplayName("요청 빈도가 제한을 초과하면 수집을 거부한다")
	void ingest_rejectsWhenRateLimitExceeded() {
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		when(rateLimiter.tryAcquire("anonymous:anon-1")).thenReturn(false);
		ClientActivityIngestService service = new ClientActivityIngestService(
			properties,
			rateLimiter,
			storeService,
			publisherProvider,
			DIRECT_EXECUTOR);
		ClientActivityEventItemRequest item = eventItem("evt-1", "ui.restaurant.viewed", null);

		assertThatThrownBy(() -> service.ingest(null, "anon-1", List.of(item)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_RATE_LIMIT_EXCEEDED.name());
		verifyNoInteractions(storeService, publisherProvider);
	}

	@Test
	@DisplayName("properties에 null 값이 포함되어도 null-safe 정규화 후 저장한다")
	void ingest_normalizesNullPropertyEntries() {
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		when(rateLimiter.tryAcquire("anonymous:anon-1")).thenReturn(true);
		ClientActivityIngestService service = new ClientActivityIngestService(
			properties,
			rateLimiter,
			storeService,
			publisherProvider,
			DIRECT_EXECUTOR);

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

		int accepted = service.ingest(null, "anon-1", List.of(item));

		assertThat(accepted).isEqualTo(1);
		ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
		verify(storeService).store(captor.capture());
		Map<String, Object> storedProperties = captor.getValue().properties();
		assertThat(storedProperties).containsEntry("restaurantId", 1L);
		assertThat(storedProperties).containsEntry("source", "CLIENT");
		assertThat(storedProperties).doesNotContainKey("subgroupId");
		assertThat(storedProperties).doesNotContainKey("");
	}

	@Test
	@DisplayName("S3 direct ingest는 publisher로만 이벤트를 전달한다")
	void ingestToS3_publishesEventsWithoutUsingDbStore() {
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		UserActivityS3SinkPublisher publisher = mock(UserActivityS3SinkPublisher.class);
		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		when(rateLimiter.tryAcquire("member:10")).thenReturn(true);
		when(publisherProvider.getIfAvailable()).thenReturn(publisher);
		ClientActivityIngestService service = new ClientActivityIngestService(
			properties,
			rateLimiter,
			storeService,
			publisherProvider,
			DIRECT_EXECUTOR);

		int accepted = service.ingestToS3(10L, null, List.of(
			eventItem("evt-1", "ui.restaurant.viewed", null),
			eventItem("evt-2", "ui.review.submitted", null)));

		assertThat(accepted).isEqualTo(2);
		verify(publisher, times(2)).sink(org.mockito.ArgumentMatchers.any(ActivityEvent.class));
		verifyNoInteractions(storeService);
	}

	@Test
	@DisplayName("S3 direct ingest 위임 큐가 포화되면 fast-fail 한다")
	void ingestToS3_rejectsWhenPublishExecutorIsSaturated() {
		AnalyticsIngestProperties properties = defaultProperties();
		ClientActivityIngestRateLimiter rateLimiter = mock(ClientActivityIngestRateLimiter.class);
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		UserActivityS3SinkPublisher publisher = mock(UserActivityS3SinkPublisher.class);
		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		Executor rejectingExecutor = command -> {
			throw new RejectedExecutionException("queue full");
		};
		when(rateLimiter.tryAcquire("member:10")).thenReturn(true);
		when(publisherProvider.getIfAvailable()).thenReturn(publisher);
		ClientActivityIngestService service = new ClientActivityIngestService(
			properties,
			rateLimiter,
			storeService,
			publisherProvider,
			rejectingExecutor);

		assertThatThrownBy(() -> service.ingestToS3(10L, null, List.of(
			eventItem("evt-1", "ui.restaurant.viewed", null))))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(AnalyticsErrorCode.ANALYTICS_INGEST_RATE_LIMIT_EXCEEDED.name());
		verifyNoInteractions(storeService, publisher);
	}

	private AnalyticsIngestProperties defaultProperties() {
		AnalyticsIngestProperties properties = new AnalyticsIngestProperties();
		properties.setMaxBatchSize(50);
		properties.setAllowlist(List.of("ui.restaurant.viewed", "ui.review.submitted", "ui.group.viewed"));
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
