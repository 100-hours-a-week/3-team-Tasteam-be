package com.tasteam.domain.analytics.ingest;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventItemRequest;
import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AnalyticsErrorCode;
import com.tasteam.infra.messagequeue.UserActivityS3SinkPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientActivityIngestService {

	private static final String DEFAULT_EVENT_VERSION = "v1";

	private final AnalyticsIngestProperties ingestProperties;
	private final ClientActivityIngestRateLimiter rateLimiter;
	private final UserActivityEventStoreService userActivityEventStoreService;
	private final ObjectProvider<UserActivityS3SinkPublisher> userActivityS3SinkPublisherProvider;
	@Qualifier("clientActivityPublishExecutor")
	private final Executor clientActivityPublishExecutor;
	private final Clock clock = Clock.systemUTC();

	public int ingest(Long memberId, String anonymousId, List<ClientActivityEventItemRequest> events) {
		List<ActivityEvent> activityEvents = prepareEvents(memberId, anonymousId, events);
		for (ActivityEvent activityEvent : activityEvents) {
			userActivityEventStoreService.store(activityEvent);
		}
		return activityEvents.size();
	}

	public int ingestToS3(Long memberId, String anonymousId, List<ClientActivityEventItemRequest> events) {
		UserActivityS3SinkPublisher userActivityS3SinkPublisher = userActivityS3SinkPublisherProvider.getIfAvailable();
		if (userActivityS3SinkPublisher == null) {
			throw new IllegalStateException("UserActivityS3SinkPublisher 빈이 없어 S3 direct ingest를 처리할 수 없습니다.");
		}
		List<ActivityEvent> activityEvents = prepareEvents(memberId, anonymousId, events);
		try {
			clientActivityPublishExecutor.execute(() -> {
				for (ActivityEvent activityEvent : activityEvents) {
					userActivityS3SinkPublisher.sink(activityEvent);
				}
			});
		} catch (RejectedExecutionException ex) {
			throw new BusinessException(AnalyticsErrorCode.ANALYTICS_INGEST_RATE_LIMIT_EXCEEDED);
		}
		return activityEvents.size();
	}

	private List<ActivityEvent> prepareEvents(Long memberId, String anonymousId,
		List<ClientActivityEventItemRequest> events) {
		validateBatch(events);
		String normalizedAnonymousId = normalizeAnonymousId(anonymousId);
		String rateLimitKey = resolveRateLimitKey(memberId, normalizedAnonymousId);

		if (!rateLimiter.tryAcquire(rateLimitKey)) {
			throw new BusinessException(AnalyticsErrorCode.ANALYTICS_INGEST_RATE_LIMIT_EXCEEDED);
		}

		Set<String> allowedEventNames = ingestProperties.allowedEventNames();
		List<ActivityEvent> activityEvents = new java.util.ArrayList<>(events.size());
		for (ClientActivityEventItemRequest eventItem : events) {
			validateAllowlist(allowedEventNames, eventItem.eventName());
			activityEvents.add(toActivityEvent(memberId, normalizedAnonymousId, eventItem));
		}
		return List.copyOf(activityEvents);
	}

	private void validateBatch(List<ClientActivityEventItemRequest> events) {
		if (events == null || events.isEmpty()) {
			throw new BusinessException(AnalyticsErrorCode.ANALYTICS_INGEST_EMPTY_BATCH);
		}
		if (events.size() > ingestProperties.validatedMaxBatchSize()) {
			throw new BusinessException(AnalyticsErrorCode.ANALYTICS_INGEST_BATCH_LIMIT_EXCEEDED);
		}
	}

	private void validateAllowlist(Set<String> allowlist, String eventName) {
		if (!allowlist.contains(eventName)) {
			throw new BusinessException(AnalyticsErrorCode.ANALYTICS_INGEST_EVENT_NOT_ALLOWED);
		}
	}

	private String resolveRateLimitKey(Long memberId, String normalizedAnonymousId) {
		if (memberId != null) {
			return "member:" + memberId;
		}
		if (!StringUtils.hasText(normalizedAnonymousId)) {
			throw new BusinessException(AnalyticsErrorCode.ANALYTICS_INGEST_ANONYMOUS_ID_REQUIRED);
		}
		return "anonymous:" + normalizedAnonymousId;
	}

	private ActivityEvent toActivityEvent(Long memberId, String anonymousId, ClientActivityEventItemRequest eventItem) {
		Objects.requireNonNull(eventItem, "eventItem은 null일 수 없습니다.");
		return new ActivityEvent(
			eventItem.eventId(),
			eventItem.eventName(),
			resolveEventVersion(eventItem.eventVersion()),
			resolveOccurredAt(eventItem.occurredAt()),
			memberId,
			memberId == null ? anonymousId : null,
			resolveProperties(eventItem.properties()));
	}

	private String resolveEventVersion(String eventVersion) {
		if (!StringUtils.hasText(eventVersion)) {
			return DEFAULT_EVENT_VERSION;
		}
		return eventVersion.trim();
	}

	private Instant resolveOccurredAt(Instant occurredAt) {
		if (occurredAt == null) {
			return Instant.now(clock);
		}
		return occurredAt;
	}

	private Map<String, Object> resolveProperties(Map<String, Object> properties) {
		if (properties == null || properties.isEmpty()) {
			return Map.of("source", "CLIENT");
		}
		java.util.LinkedHashMap<String, Object> normalized = new java.util.LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			if (!StringUtils.hasText(entry.getKey())) {
				continue;
			}
			if (entry.getValue() == null) {
				continue;
			}
			normalized.put(entry.getKey(), entry.getValue());
		}
		normalized.put("source", "CLIENT");
		return Map.copyOf(normalized);
	}

	private String normalizeAnonymousId(String anonymousId) {
		if (!StringUtils.hasText(anonymousId)) {
			return null;
		}
		return anonymousId.trim();
	}
}
