package com.tasteam.domain.analytics.ingest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;
import com.tasteam.infra.messagequeue.UserActivityS3SinkPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsIngestStartupValidator implements SmartInitializingSingleton {

	private static final Profiles FAIL_FAST_PROFILES = Profiles.of("dev", "stg", "prod");

	private final Environment environment;
	private final MessageQueueProperties messageQueueProperties;
	private final ObjectProvider<UserActivityS3SinkPublisher> userActivityS3SinkPublisherProvider;

	@Override
	public void afterSingletonsInstantiated() {
		if (!environment.acceptsProfiles(FAIL_FAST_PROFILES)) {
			return;
		}
		if (messageQueueProperties.effectiveProviderType() == MessageQueueProviderType.NONE) {
			throw new IllegalStateException(
				"analytics ingest는 dev/stg/prod 환경에서 활성 message queue provider가 필요합니다.");
		}
		if (userActivityS3SinkPublisherProvider.getIfAvailable() == null) {
			throw new IllegalStateException(
				"analytics ingest는 dev/stg/prod 환경에서 UserActivityS3SinkPublisher 빈이 필요합니다.");
		}
	}
}
