package com.tasteam.infra.analytics.posthog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.dispatch.UserActivityDispatchSink;
import com.tasteam.domain.analytics.dispatch.UserActivityDispatchTarget;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.posthog", name = "enabled", havingValue = "true")
public class PosthogSink implements UserActivityDispatchSink {

	private final PosthogClient posthogClient;

	@Override
	public UserActivityDispatchTarget target() {
		return UserActivityDispatchTarget.POSTHOG;
	}

	@Override
	public void dispatch(ActivityEvent event) {
		posthogClient.capture(event);
	}
}
