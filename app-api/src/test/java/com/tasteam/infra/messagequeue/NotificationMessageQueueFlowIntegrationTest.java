package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.MessageQueueFlowTest;
import com.tasteam.domain.group.event.GroupMemberJoinedEvent;
import com.tasteam.domain.group.event.GroupMemberJoinedMessagePayload;
import com.tasteam.domain.group.event.GroupMemberJoinedMqPublisher;

import jakarta.annotation.Resource;

@MessageQueueFlowTest
@SpringBootTest(classes = NotificationMessageQueueFlowIntegrationTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("[통합](Group) GroupMemberJoinedMqPublisher 통합 테스트")
class NotificationMessageQueueFlowIntegrationTest {

	@Resource
	private ApplicationEventPublisher applicationEventPublisher;

	@Resource
	private MessageQueueProducer messageQueueProducer;

	@Resource
	private ObjectMapper objectMapper;

	@Resource
	private TopicNamingPolicy topicNamingPolicy;

	@Test
	@DisplayName("GroupMemberJoined 이벤트 발행 시 GROUP_MEMBER_JOINED 토픽으로 MQ publish된다")
	void groupMemberJoinedEvent_publishesToMq() throws Exception {
		// given
		ArgumentCaptor<QueueMessage> publishedMessageCaptor = ArgumentCaptor.forClass(QueueMessage.class);

		// when
		applicationEventPublisher.publishEvent(new GroupMemberJoinedEvent(10L, 20L, "스터디 그룹",
			Instant.parse("2026-02-15T00:00:00Z")));

		// then
		verify(messageQueueProducer).publish(publishedMessageCaptor.capture());
		QueueMessage publishedMessage = publishedMessageCaptor.getValue();
		assertThat(publishedMessage.topic()).isEqualTo(QueueTopic.GROUP_MEMBER_JOINED.defaultMainTopic());
		assertThat(publishedMessage.key()).isEqualTo("20");
		assertThat(publishedMessage.headers()).containsEntry("eventType", "GroupMemberJoinedEvent");

		GroupMemberJoinedMessagePayload payload = objectMapper.treeToValue(
			publishedMessage.payload(),
			GroupMemberJoinedMessagePayload.class);
		assertThat(payload.groupId()).isEqualTo(10L);
		assertThat(payload.memberId()).isEqualTo(20L);
		assertThat(payload.groupName()).isEqualTo("스터디 그룹");
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
		TopicNamingPolicy topicNamingPolicy() {
			return new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		}

		@Bean
		MessageQueueProducer messageQueueProducer() {
			return Mockito.mock(MessageQueueProducer.class);
		}

		@Bean
		GroupMemberJoinedMqPublisher groupMemberJoinedMqPublisher(
			MessageQueueProducer messageQueueProducer,
			MessageQueueProperties messageQueueProperties,
			TopicNamingPolicy topicNamingPolicy,
			ObjectMapper objectMapper) {
			return new GroupMemberJoinedMqPublisher(messageQueueProducer, messageQueueProperties,
				topicNamingPolicy,
				objectMapper);
		}
	}
}
