package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivityEventMapper;
import com.tasteam.domain.analytics.api.ActivitySink;
import com.tasteam.domain.analytics.application.ActivityDomainEventListener;
import com.tasteam.domain.analytics.application.ActivityEventMapperRegistry;
import com.tasteam.domain.analytics.application.ActivityEventOrchestrator;
import com.tasteam.domain.analytics.application.mapper.ReviewCreatedActivityEventMapper;
import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;
import com.tasteam.domain.review.event.ReviewCreatedEvent;

import jakarta.annotation.Resource;

@SpringBootTest(classes = UserActivityMessageQueueFlowIntegrationTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("User Activity MQ 연동 통합 테스트")
class UserActivityMessageQueueFlowIntegrationTest {

	@Resource
	private ApplicationEventPublisher applicationEventPublisher;

	@Resource
	private MessageQueueProducer messageQueueProducer;

	@Resource
	private MessageQueueConsumer messageQueueConsumer;

	@Resource
	private UserActivityEventStoreService userActivityEventStoreService;

	@Resource
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("ReviewCreated 이벤트를 발행하면 USER_ACTIVITY 메시지를 발행하고 소비 핸들러로 저장된다")
	void reviewCreatedEvent_publishAndConsumeUserActivity() throws Exception {
		// given
		ArgumentCaptor<MessageQueueMessage> publishedMessageCaptor = ArgumentCaptor.forClass(MessageQueueMessage.class);
		ArgumentCaptor<MessageQueueSubscription> subscriptionCaptor = ArgumentCaptor
			.forClass(MessageQueueSubscription.class);
		@SuppressWarnings("unchecked") ArgumentCaptor<MessageQueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(
				MessageQueueMessageHandler.class);
		verify(messageQueueConsumer).subscribe(subscriptionCaptor.capture(), handlerCaptor.capture());

		// when
		applicationEventPublisher.publishEvent(new ReviewCreatedEvent(130L));

		// then
		verify(messageQueueProducer).publish(publishedMessageCaptor.capture());
		MessageQueueMessage published = publishedMessageCaptor.getValue();
		assertThat(published.topic()).isEqualTo(MessageQueueTopics.USER_ACTIVITY);
		assertThat(published.messageId()).isNotBlank();

		ActivityEvent payload = objectMapper.readValue(published.payload(), ActivityEvent.class);
		assertThat(payload.eventName()).isEqualTo("review.created");
		assertThat(((Number)payload.properties().get("restaurantId")).longValue()).isEqualTo(130L);

		assertThat(subscriptionCaptor.getValue().topic()).isEqualTo(MessageQueueTopics.USER_ACTIVITY);
		assertThat(subscriptionCaptor.getValue().consumerGroup()).isEqualTo("tasteam-api-user-activity");

		handlerCaptor.getValue().handle(MessageQueueMessage.of(
			MessageQueueTopics.USER_ACTIVITY,
			published.key(),
			published.payload()));
		verify(userActivityEventStoreService).store(any(ActivityEvent.class));
	}

	@Configuration
	static class TestConfig {

		@Bean
		MessageQueueProperties messageQueueProperties() {
			MessageQueueProperties properties = new MessageQueueProperties();
			properties.setEnabled(true);
			properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
			properties.setDefaultConsumerGroup("tasteam-api");
			return properties;
		}

		@Bean
		ObjectMapper objectMapper() {
			return JsonMapper.builder().findAndAddModules().build();
		}

		@Bean
		MessageQueueProducer messageQueueProducer() {
			return Mockito.mock(MessageQueueProducer.class);
		}

		@Bean
		MessageQueueConsumer messageQueueConsumer() {
			return Mockito.mock(MessageQueueConsumer.class);
		}

		@Bean
		UserActivityEventStoreService userActivityEventStoreService() {
			return Mockito.mock(UserActivityEventStoreService.class);
		}

		@Bean
		ActivityEventMapper<?> reviewCreatedActivityEventMapper() {
			return new ReviewCreatedActivityEventMapper();
		}

		@Bean
		UserActivityMessageQueuePublisher userActivityMessageQueuePublisher(
			MessageQueueProducer messageQueueProducer,
			MessageQueueProperties messageQueueProperties,
			ObjectMapper objectMapper,
			UserActivitySourceOutboxService outboxService) {
			return new UserActivityMessageQueuePublisher(
				messageQueueProducer,
				messageQueueProperties,
				objectMapper,
				outboxService);
		}

		@Bean
		UserActivitySourceOutboxService userActivitySourceOutboxService() {
			return Mockito.mock(UserActivitySourceOutboxService.class);
		}

		@Bean
		UserActivitySourceOutboxSink userActivitySourceOutboxSink(UserActivitySourceOutboxService outboxService) {
			return new UserActivitySourceOutboxSink(outboxService);
		}

		@Bean
		ActivityEventMapperRegistry activityEventMapperRegistry(
			java.util.List<ActivityEventMapper<?>> mappers) {
			return new ActivityEventMapperRegistry(mappers);
		}

		@Bean
		ActivityEventOrchestrator activityEventOrchestrator(
			ActivityEventMapperRegistry mapperRegistry,
			java.util.List<ActivitySink> sinks) {
			return new ActivityEventOrchestrator(mapperRegistry, sinks);
		}

		@Bean
		ActivityDomainEventListener activityDomainEventListener(ActivityEventOrchestrator orchestrator) {
			return new ActivityDomainEventListener(orchestrator);
		}

		@Bean
		UserActivityMessageQueueConsumerRegistrar userActivityMessageQueueConsumerRegistrar(
			MessageQueueConsumer messageQueueConsumer,
			MessageQueueProperties messageQueueProperties,
			UserActivityEventStoreService userActivityEventStoreService,
			ObjectMapper objectMapper) {
			return new UserActivityMessageQueueConsumerRegistrar(
				messageQueueConsumer,
				messageQueueProperties,
				userActivityEventStoreService,
				objectMapper);
		}
	}
}
