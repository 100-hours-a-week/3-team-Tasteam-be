package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

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
import com.tasteam.domain.restaurant.service.analysis.RestaurantReviewAnalysisService;
import com.tasteam.domain.review.event.ReviewCreatedEvent;

import jakarta.annotation.Resource;

@SpringBootTest(classes = ReviewCreatedMessageQueueFlowIntegrationTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("ReviewCreated MQ 연동 통합 테스트")
class ReviewCreatedMessageQueueFlowIntegrationTest {

	private static final String CONSUMER_GROUP = "tasteam-api";

	@Resource
	private ApplicationEventPublisher applicationEventPublisher;

	@Resource
	private MessageQueueProducer messageQueueProducer;

	@Resource
	private MessageQueueConsumer messageQueueConsumer;

	@Resource
	private RestaurantReviewAnalysisService restaurantReviewAnalysisService;

	@Resource
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("도메인 이벤트를 발행하면 MQ publish와 consumer 처리까지 이어진다")
	void reviewCreatedEvent_publishAndConsume() throws Exception {
		// given
		ArgumentCaptor<MessageQueueMessage> publishedMessageCaptor = ArgumentCaptor.forClass(MessageQueueMessage.class);
		ArgumentCaptor<MessageQueueSubscription> subscriptionCaptor = ArgumentCaptor
			.forClass(MessageQueueSubscription.class);
		@SuppressWarnings("unchecked") ArgumentCaptor<MessageQueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(
				MessageQueueMessageHandler.class);
		verify(messageQueueConsumer).subscribe(subscriptionCaptor.capture(), handlerCaptor.capture());

		// when
		applicationEventPublisher.publishEvent(new ReviewCreatedEvent(123L));

		// then
		verify(messageQueueProducer).publish(publishedMessageCaptor.capture());
		MessageQueueMessage publishedMessage = publishedMessageCaptor.getValue();
		assertThat(publishedMessage.topic()).isEqualTo(MessageQueueTopics.REVIEW_CREATED);
		assertThat(publishedMessage.key()).isEqualTo("123");
		assertThat(publishedMessage.headers()).containsEntry("eventType", "ReviewCreatedEvent");

		ReviewCreatedMessagePayload payload = objectMapper.readValue(publishedMessage.payload(),
			ReviewCreatedMessagePayload.class);
		assertThat(payload.restaurantId()).isEqualTo(123L);

		MessageQueueSubscription subscription = subscriptionCaptor.getValue();
		assertThat(subscription.topic()).isEqualTo(MessageQueueTopics.REVIEW_CREATED);
		assertThat(subscription.consumerGroup()).isEqualTo(CONSUMER_GROUP);

		handlerCaptor.getValue().handle(MessageQueueMessage.of(
			MessageQueueTopics.REVIEW_CREATED,
			"123",
			objectMapper.writeValueAsBytes(new ReviewCreatedMessagePayload(123L))));

		verify(restaurantReviewAnalysisService).onReviewCreated(123L);
	}

	@Test
	@DisplayName("잘못된 payload를 수신하면 예외를 반환한다")
	void consumerHandler_withInvalidPayload_throwsException() {
		// given
		ArgumentCaptor<MessageQueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(MessageQueueMessageHandler.class);
		verify(messageQueueConsumer).subscribe(any(MessageQueueSubscription.class), handlerCaptor.capture());

		// when & then
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> handlerCaptor.getValue().handle(
			MessageQueueMessage.of(
				MessageQueueTopics.REVIEW_CREATED,
				"123",
				"{\"restaurantId\":\"invalid\"}".getBytes(StandardCharsets.UTF_8))))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("역직렬화");
	}

	@Configuration
	static class TestConfig {

		@Bean
		MessageQueueProperties messageQueueProperties() {
			MessageQueueProperties properties = new MessageQueueProperties();
			properties.setEnabled(true);
			properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
			properties.setDefaultConsumerGroup(CONSUMER_GROUP);
			return properties;
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
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
		RestaurantReviewAnalysisService restaurantReviewAnalysisService() {
			return Mockito.mock(RestaurantReviewAnalysisService.class);
		}

		@Bean
		ReviewCreatedMessageQueuePublisher reviewCreatedMessageQueuePublisher(
			MessageQueueProducer messageQueueProducer,
			MessageQueueProperties messageQueueProperties,
			ObjectMapper objectMapper) {
			return new ReviewCreatedMessageQueuePublisher(messageQueueProducer, messageQueueProperties, objectMapper);
		}

		@Bean
		ReviewCreatedMessageQueueConsumerRegistrar reviewCreatedMessageQueueConsumerRegistrar(
			MessageQueueConsumer messageQueueConsumer,
			MessageQueueProperties messageQueueProperties,
			RestaurantReviewAnalysisService restaurantReviewAnalysisService,
			ObjectMapper objectMapper) {
			return new ReviewCreatedMessageQueueConsumerRegistrar(
				messageQueueConsumer,
				messageQueueProperties,
				restaurantReviewAnalysisService,
				objectMapper);
		}
	}
}
