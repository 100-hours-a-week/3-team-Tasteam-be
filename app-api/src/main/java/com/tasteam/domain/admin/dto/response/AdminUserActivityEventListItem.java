package com.tasteam.domain.admin.dto.response;

import java.time.Instant;
import java.util.Map;

import com.tasteam.domain.analytics.persistence.UserActivityEventEntity;

public record AdminUserActivityEventListItem(
	Long id,
	String eventId,
	String eventName,
	String eventVersion,
	Instant occurredAt,
	Long memberId,
	String anonymousId,
	String sessionId,
	String source,
	String requestPath,
	String requestMethod,
	String deviceId,
	String platform,
	String appVersion,
	String locale,
	Map<String, Object> properties,
	Instant createdAt) {

	public static AdminUserActivityEventListItem from(UserActivityEventEntity entity) {
		return new AdminUserActivityEventListItem(
			entity.getId(),
			entity.getEventId(),
			entity.getEventName(),
			entity.getEventVersion(),
			entity.getOccurredAt(),
			entity.getMemberId(),
			entity.getAnonymousId(),
			entity.getSessionId(),
			entity.getSource(),
			entity.getRequestPath(),
			entity.getRequestMethod(),
			entity.getDeviceId(),
			entity.getPlatform(),
			entity.getAppVersion(),
			entity.getLocale(),
			entity.getProperties(),
			entity.getCreatedAt());
	}
}
