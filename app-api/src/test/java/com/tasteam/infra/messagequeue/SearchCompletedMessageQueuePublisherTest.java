package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.search.event.SearchCompletedEvent;

@UnitTest
@DisplayName("[유닛](Search) SearchCompletedMessageQueuePublisher 단위 테스트")
class SearchCompletedMessageQueuePublisherTest {

	@Test
	@DisplayName("provider가 none이면 메시지를 발행하지 않는다")
	void onSearchCompleted_withNoneProvider_skipsPublish() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		ObjectMapper objectMapper = new ObjectMapper();
		SearchCompletedMessageQueuePublisher publisher = new SearchCompletedMessageQueuePublisher(
			producer,
			properties,
			topicNamingPolicy,
			objectMapper);

		// when
		publisher.onSearchCompleted(new SearchCompletedEvent(1L, "치킨", 2, 3));

		// then
		verifyNoInteractions(producer);
	}

	@Test
	@DisplayName("provider가 redis-stream이면 SearchCompleted 이벤트를 MQ로 발행한다")
	void onSearchCompleted_withRedisStreamProvider_publishesMessage() throws Exception {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		ObjectMapper objectMapper = new ObjectMapper();
		SearchCompletedMessageQueuePublisher publisher = new SearchCompletedMessageQueuePublisher(
			producer,
			properties,
			topicNamingPolicy,
			objectMapper);

		// when
		publisher.onSearchCompleted(new SearchCompletedEvent(1L, "치킨", 2, 3));

		// then
		ArgumentCaptor<QueueMessage> messageCaptor = ArgumentCaptor.forClass(QueueMessage.class);
		verify(producer).publish(messageCaptor.capture());

		QueueMessage message = messageCaptor.getValue();
		assertThat(message.topic()).isEqualTo(QueueTopic.SEARCH_COMPLETED.defaultMainTopic());
		assertThat(message.key()).isEqualTo("1");
		assertThat(message.headers()).containsEntry("eventType", "SearchCompletedEvent");
		assertThat(message.occurredAt()).isNotNull();

		SearchCompletedMessagePayload payload = objectMapper.treeToValue(
			message.payload(),
			SearchCompletedMessagePayload.class);
		assertThat(payload.memberId()).isEqualTo(1L);
		assertThat(payload.keyword()).isEqualTo("치킨");
		assertThat(payload.groupResultCount()).isEqualTo(2);
		assertThat(payload.restaurantResultCount()).isEqualTo(3);
	}

	@Test
	@DisplayName("memberId가 없으면 anonymous key로 메시지를 발행한다")
	void onSearchCompleted_whenMemberIsAnonymous_publishesAnonymousKey() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		ObjectMapper objectMapper = new ObjectMapper();
		SearchCompletedMessageQueuePublisher publisher = new SearchCompletedMessageQueuePublisher(
			producer,
			properties,
			topicNamingPolicy,
			objectMapper);

		// when
		publisher.onSearchCompleted(new SearchCompletedEvent(null, "치킨", 2, 3));

		// then
		ArgumentCaptor<QueueMessage> messageCaptor = ArgumentCaptor.forClass(QueueMessage.class);
		verify(producer).publish(messageCaptor.capture());
		assertThat(messageCaptor.getValue().key()).isEqualTo("anonymous");
	}

	@Test
	@DisplayName("직렬화에 실패하면 예외를 반환한다")
	void onSearchCompleted_withSerializationFailure_throwsException() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		when(objectMapper.valueToTree(org.mockito.ArgumentMatchers.any()))
			.thenThrow(new IllegalArgumentException("failed"));
		SearchCompletedMessageQueuePublisher publisher = new SearchCompletedMessageQueuePublisher(
			producer,
			properties,
			topicNamingPolicy,
			objectMapper);

		// when & then
		assertThatThrownBy(() -> publisher.onSearchCompleted(new SearchCompletedEvent(1L, "치킨", 2, 3)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("직렬화");
	}
}
