package com.tasteam.infra.analytics.posthog;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.tasteam.domain.analytics.api.ActivityEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.posthog", name = "enabled", havingValue = "true")
public class PosthogClient {

	private final RestClient posthogRestClient;
	private final PosthogProperties properties;

	public void capture(ActivityEvent event) {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new IllegalStateException("PostHog API Key가 비어 있어 capture를 수행할 수 없습니다.");
		}

		PosthogCaptureRequest request = new PosthogCaptureRequest(
			properties.getApiKey(),
			event.eventName(),
			event.eventId(),
			event.occurredAt(),
			buildProperties(event));

		posthogRestClient.post()
			.uri("/capture/")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toBodilessEntity();
	}

	private Map<String, Object> buildProperties(ActivityEvent event) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("event_id", event.eventId());
		properties.put("event_version", event.eventVersion());
		properties.put("occurred_at", event.occurredAt());
		if (event.memberId() != null) {
			properties.put("member_id", event.memberId());
		}
		if (event.anonymousId() != null) {
			properties.put("anonymous_id", event.anonymousId());
		}
		if (event.properties() != null && !event.properties().isEmpty()) {
			properties.putAll(event.properties());
		}
		return Map.copyOf(properties);
	}
}
