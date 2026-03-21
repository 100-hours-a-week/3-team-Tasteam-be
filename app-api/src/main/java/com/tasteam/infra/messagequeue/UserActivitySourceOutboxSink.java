package com.tasteam.infra.messagequeue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivitySink;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "tasteam.analytics.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UserActivitySourceOutboxSink implements ActivitySink {

	private final UserActivitySourceOutboxService outboxService;

	@Override
	public String sinkType() {
		return "USER_ACTIVITY_SOURCE_OUTBOX";
	}

	@Override
	public void sink(ActivityEvent event) {
		outboxService.enqueue(event);
	}
}
