package com.tasteam.domain.analytics.ingest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientActivityIngestRouteValidator {

	private final AnalyticsIngestProperties ingestProperties;
	private final MessageQueueProperties messageQueueProperties;

	@PostConstruct
	void validate() {
		if (!"s3".equals(ingestProperties.validatedRoute())) {
			return;
		}
		if (messageQueueProperties.effectiveProviderType() == MessageQueueProviderType.KAFKA) {
			return;
		}
		throw new IllegalStateException(
			"tasteam.analytics.ingest.route=s3 requires tasteam.message-queue.enabled=true and tasteam.message-queue.provider=kafka");
	}
}
