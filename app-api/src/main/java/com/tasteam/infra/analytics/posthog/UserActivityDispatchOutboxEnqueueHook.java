package com.tasteam.infra.analytics.posthog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.dispatch.UserActivityDispatchOutboxService;
import com.tasteam.domain.analytics.dispatch.UserActivityDispatchTarget;
import com.tasteam.domain.analytics.persistence.UserActivityStoredHook;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.posthog", name = "enabled", havingValue = "true")
public class UserActivityDispatchOutboxEnqueueHook implements UserActivityStoredHook {

	private final UserActivityDispatchOutboxService dispatchOutboxService;

	@Override
	public String hookType() {
		return "USER_ACTIVITY_POSTHOG_OUTBOX_ENQUEUE";
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void afterStored(ActivityEvent event) {
		dispatchOutboxService.enqueue(event, UserActivityDispatchTarget.POSTHOG);
	}
}
