package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;

@UnitTest
@DisplayName("[유닛](UserActivityS3) UserActivityS3SinkPublisher 단위 테스트")
class UserActivityS3SinkPublisherTest {

	@Test
	@DisplayName("ActivityEvent를 USER_ACTIVITY_S3_INGEST 토픽으로 변환하여 발행한다")
	void sink_publishesToS3IngestTopic() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		UserActivityS3SinkPublisher publisher = new UserActivityS3SinkPublisher(
			producer,
			properties,
			topicNamingPolicy,
			JsonMapper.builder().findAndAddModules().build());

		ActivityEvent event = new ActivityEvent(
			"evt-s3-1",
			"ui.restaurant.clicked",
			"v1",
			Instant.parse("2026-03-11T10:00:00Z"),
			101L,
			null,
			Map.of(
				"restaurantId", 42L,
				"platform", "IOS",
				"sessionId", "sess-abc",
				"diningType", "DINE_IN",
				"distanceBucket", "NEAR",
				"weatherBucket", "CLEAR"));

		// when
		publisher.sink(event);

		// then
		ArgumentCaptor<QueueMessage> captor = ArgumentCaptor.forClass(QueueMessage.class);
		verify(producer).publish(captor.capture());
		QueueMessage message = captor.getValue();

		assertThat(message.topic()).isEqualTo(topicNamingPolicy.main(QueueTopic.USER_ACTIVITY_S3_INGEST));
		assertThat(message.key()).isEqualTo("101");
		assertThat(message.messageId()).isEqualTo("evt-s3-1");
		assertThat(message.occurredAt()).isEqualTo(Instant.parse("2026-03-11T10:00:00Z"));
		assertThat(message.headers())
			.containsEntry("eventType", "UserActivityS3Event")
			.containsEntry("eventName", "ui.restaurant.clicked")
			.containsEntry("schemaVersion", "v1");
	}

	@Test
	@DisplayName("memberId가 없으면 anonymousId를 메시지 키로 사용한다")
	void sink_usesAnonymousIdAsKeyWhenNoMemberId() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		UserActivityS3SinkPublisher publisher = new UserActivityS3SinkPublisher(
			producer,
			properties,
			topicNamingPolicy,
			JsonMapper.builder().findAndAddModules().build());

		ActivityEvent event = new ActivityEvent(
			"evt-s3-2",
			"ui.page.viewed",
			"v1",
			Instant.parse("2026-03-11T10:00:00Z"),
			null,
			"anon-xyz",
			Map.of());

		// when
		publisher.sink(event);

		// then
		ArgumentCaptor<QueueMessage> captor = ArgumentCaptor.forClass(QueueMessage.class);
		verify(producer).publish(captor.capture());
		assertThat(captor.getValue().key()).isEqualTo("anon-xyz");
	}

	@Test
	@DisplayName("memberId와 anonymousId가 모두 없으면 key는 null이다")
	void sink_nullKeyWhenNoMemberIdAndAnonymousId() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		UserActivityS3SinkPublisher publisher = new UserActivityS3SinkPublisher(
			producer,
			properties,
			topicNamingPolicy,
			JsonMapper.builder().findAndAddModules().build());

		ActivityEvent event = new ActivityEvent(
			"evt-s3-3",
			"ui.page.viewed",
			"v1",
			Instant.parse("2026-03-11T10:00:00Z"),
			null,
			null,
			Map.of());

		// when
		publisher.sink(event);

		// then
		ArgumentCaptor<QueueMessage> captor = ArgumentCaptor.forClass(QueueMessage.class);
		verify(producer).publish(captor.capture());
		assertThat(captor.getValue().key()).isNull();
	}

	@Test
	@DisplayName("provider가 none이면 발행을 건너뛴다")
	void sink_skipsWhenProviderIsNone() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.NONE.value());
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		UserActivityS3SinkPublisher publisher = new UserActivityS3SinkPublisher(
			producer,
			properties,
			topicNamingPolicy,
			JsonMapper.builder().findAndAddModules().build());

		ActivityEvent event = new ActivityEvent(
			"evt-s3-4",
			"ui.page.viewed",
			"v1",
			Instant.parse("2026-03-11T10:00:00Z"),
			null,
			"anon-1",
			Map.of());

		// when
		publisher.sink(event);

		// then
		verifyNoInteractions(producer);
	}

	@Test
	@DisplayName("ActivityEvent.properties의 14개 컬럼이 UserActivityS3Event로 올바르게 매핑된다")
	void sink_mapsAllS3EventColumns() throws Exception {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		var objectMapper = JsonMapper.builder().findAndAddModules().build();
		UserActivityS3SinkPublisher publisher = new UserActivityS3SinkPublisher(
			producer,
			properties,
			topicNamingPolicy,
			objectMapper);

		ActivityEvent event = new ActivityEvent(
			"evt-s3-5",
			"ui.restaurant.clicked",
			"v1",
			Instant.parse("2026-03-11T10:00:00Z"),
			200L,
			null,
			Map.of(
				"restaurantId", 99L,
				"recommendationId", "rec-001",
				"platform", "ANDROID",
				"sessionId", "sess-def",
				"diningType", "DELIVERY",
				"distanceBucket", "FAR",
				"weatherBucket", "RAIN"));

		// when
		publisher.sink(event);

		// then
		ArgumentCaptor<QueueMessage> captor = ArgumentCaptor.forClass(QueueMessage.class);
		verify(producer).publish(captor.capture());

		UserActivityS3Event s3Event = objectMapper.treeToValue(captor.getValue().payload(), UserActivityS3Event.class);
		assertThat(s3Event.eventId()).isEqualTo("evt-s3-5");
		assertThat(s3Event.eventName()).isEqualTo("ui.restaurant.clicked");
		assertThat(s3Event.eventVersion()).isEqualTo("v1");
		assertThat(s3Event.occurredAt()).isEqualTo(Instant.parse("2026-03-11T10:00:00Z"));
		assertThat(s3Event.memberId()).isEqualTo(200L);
		assertThat(s3Event.anonymousId()).isNull();
		assertThat(s3Event.restaurantId()).isEqualTo(99L);
		assertThat(s3Event.recommendationId()).isEqualTo("rec-001");
		assertThat(s3Event.platform()).isEqualTo("ANDROID");
		assertThat(s3Event.sessionId()).isEqualTo("sess-def");
		assertThat(s3Event.diningType()).isEqualTo("DELIVERY");
		assertThat(s3Event.distanceBucket()).isEqualTo("FAR");
		assertThat(s3Event.weatherBucket()).isEqualTo("RAIN");
		assertThat(s3Event.createdAt()).isNotNull();
	}
}
