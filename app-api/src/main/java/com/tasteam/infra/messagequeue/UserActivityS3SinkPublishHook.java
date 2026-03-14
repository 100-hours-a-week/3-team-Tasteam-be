package com.tasteam.infra.messagequeue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.persistence.UserActivityStoredHook;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "provider", havingValue = "kafka")
public class UserActivityS3SinkPublishHook implements UserActivityStoredHook {

	private final UserActivityS3SinkPublisher userActivityS3SinkPublisher;

	@Override
	public String hookType() {
		return "USER_ACTIVITY_S3_SINK_PUBLISH";
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void afterStored(ActivityEvent event) {
		userActivityS3SinkPublisher.sink(event);
	}
}
