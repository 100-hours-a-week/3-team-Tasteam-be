package com.tasteam.domain.analytics.ingest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.UserActivityS3SinkPublisher;

@UnitTest
@DisplayName("[유닛](Analytics) AnalyticsIngestStartupValidator 단위 테스트")
class AnalyticsIngestStartupValidatorTest {

	@Test
	@DisplayName("local/test 외 프로필이 아니면 fail-fast 검증을 건너뛴다")
	void afterSingletonsInstantiated_skipsOutsideFailFastProfiles() {
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

		AnalyticsIngestStartupValidator validator = new AnalyticsIngestStartupValidator(
			environment,
			new MessageQueueProperties(),
			emptyPublisherProvider());

		assertThatCode(validator::afterSingletonsInstantiated).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("prod 계열 프로필에서 message queue provider가 비활성이면 기동을 차단한다")
	void afterSingletonsInstantiated_failsWhenMqProviderDisabled() {
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

		AnalyticsIngestStartupValidator validator = new AnalyticsIngestStartupValidator(
			environment,
			new MessageQueueProperties(),
			emptyPublisherProvider());

		assertThatThrownBy(validator::afterSingletonsInstantiated)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("message queue provider");
	}

	@Test
	@DisplayName("prod 계열 프로필에서 publisher 빈이 없으면 기동을 차단한다")
	void afterSingletonsInstantiated_failsWhenPublisherBeanMissing() {
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

		MessageQueueProperties messageQueueProperties = new MessageQueueProperties();
		messageQueueProperties.setEnabled(true);
		messageQueueProperties.setProvider("kafka");

		AnalyticsIngestStartupValidator validator = new AnalyticsIngestStartupValidator(
			environment,
			messageQueueProperties,
			emptyPublisherProvider());

		assertThatThrownBy(validator::afterSingletonsInstantiated)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("UserActivityS3SinkPublisher");
	}

	@Test
	@DisplayName("prod 계열 프로필에서 queue provider와 publisher 빈이 준비되면 통과한다")
	void afterSingletonsInstantiated_passesWhenMqReady() {
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

		MessageQueueProperties messageQueueProperties = new MessageQueueProperties();
		messageQueueProperties.setEnabled(true);
		messageQueueProperties.setProvider("kafka");

		ObjectProvider<UserActivityS3SinkPublisher> publisherProvider = mock(ObjectProvider.class);
		when(publisherProvider.getIfAvailable()).thenReturn(mock(UserActivityS3SinkPublisher.class));

		AnalyticsIngestStartupValidator validator = new AnalyticsIngestStartupValidator(
			environment,
			messageQueueProperties,
			publisherProvider);

		assertThatCode(validator::afterSingletonsInstantiated).doesNotThrowAnyException();
	}

	private ObjectProvider<UserActivityS3SinkPublisher> emptyPublisherProvider() {
		return fixedPublisherProvider(null);
	}

	private ObjectProvider<UserActivityS3SinkPublisher> fixedPublisherProvider(UserActivityS3SinkPublisher publisher) {
		return new ObjectProvider<>() {
			@Override
			public UserActivityS3SinkPublisher getObject(Object... args) {
				return publisher;
			}

			@Override
			public UserActivityS3SinkPublisher getIfAvailable() {
				return publisher;
			}

			@Override
			public UserActivityS3SinkPublisher getIfUnique() {
				return publisher;
			}

			@Override
			public UserActivityS3SinkPublisher getObject() {
				return publisher;
			}

			@Override
			public Iterator<UserActivityS3SinkPublisher> iterator() {
				return Collections.emptyIterator();
			}
		};
	}
}
