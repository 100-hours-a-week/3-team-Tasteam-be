package com.tasteam.infra.messagequeue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.restaurant.service.analysis.RestaurantReviewAnalysisService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class ReviewCreatedMessageQueueConsumerRegistrar {

	private final MessageQueueConsumer messageQueueConsumer;
	private final MessageQueueProperties messageQueueProperties;
	private final RestaurantReviewAnalysisService restaurantReviewAnalysisService;
	private final ObjectMapper objectMapper;

	private MessageQueueSubscription subscription;

	@PostConstruct
	public void subscribe() {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			log.info("메시지큐 provider가 none이라 ReviewCreated 구독 등록을 건너뜁니다");
			return;
		}

		subscription = new MessageQueueSubscription(
			MessageQueueTopics.REVIEW_CREATED,
			messageQueueProperties.getDefaultConsumerGroup(),
			"review-analysis-" + UUID.randomUUID());

		messageQueueConsumer.subscribe(subscription, message -> {
			ReviewCreatedMessagePayload payload = deserializePayload(message.payload());
			restaurantReviewAnalysisService.onReviewCreated(payload.restaurantId());
		});
	}

	@PreDestroy
	public void unsubscribe() {
		if (subscription != null) {
			messageQueueConsumer.unsubscribe(subscription);
		}
	}

	private ReviewCreatedMessagePayload deserializePayload(byte[] payload) {
		try {
			return objectMapper.readValue(payload, ReviewCreatedMessagePayload.class);
		} catch (IOException ex) {
			String payloadAsString = new String(payload, StandardCharsets.UTF_8);
			throw new IllegalArgumentException("ReviewCreated 메시지 역직렬화에 실패했습니다. payload=" + payloadAsString, ex);
		}
	}

}
