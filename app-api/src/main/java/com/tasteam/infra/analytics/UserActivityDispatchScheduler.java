package com.tasteam.infra.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.dispatch.UserActivityDispatchOutboxDispatcher;
import com.tasteam.domain.analytics.dispatch.UserActivityDispatchResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.posthog", name = "enabled", havingValue = "true")
public class UserActivityDispatchScheduler {

	private final UserActivityDispatchOutboxDispatcher dispatcher;

	@Scheduled(fixedDelayString = "${tasteam.analytics.dispatch.fixed-delay:PT1M}")
	public void dispatchPendingEvents() {
		UserActivityDispatchResult result = dispatcher.dispatchPendingPosthog();
		if (result.processedCount() == 0) {
			return;
		}
		log.info("User Activity dispatch 배치 완료. processed={}, success={}, failed={}, circuitOpen={}",
			result.processedCount(), result.successCount(), result.failedCount(), result.circuitOpen());
	}
}
