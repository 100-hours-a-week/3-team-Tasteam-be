package com.tasteam.infra.messagequeue;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.context.EmbeddedKafka;

import com.tasteam.config.annotation.MessageQueueFlowTest;
import com.tasteam.domain.analytics.api.ActivityEvent;

/**
 * {@link UserActivityS3SinkPublisher} end-to-end 플로우 테스트.
 *
 * <p>Kafka 브로커 의존성이 있어 로컬 Kafka 없이 {@code @EmbeddedKafka}로 검증한다.
 *
 * <h3>TODO: 테스트 구현 가이드</h3>
 * <ol>
 *   <li>{@link ActivityEvent} 샘플 생성 → {@code publisher.sink()} 호출</li>
 *   <li>KafkaConsumer로 {@code evt.user-activity.s3-ingest.v1} 토픽에서 메시지 수신</li>
 *   <li>{@code QueueMessageEnvelope} 역직렬화 → payload 추출</li>
 *   <li>{@link UserActivityS3Event} 14개 컬럼 전체 검증</li>
 *   <li>key = memberId 또는 anonymousId 검증</li>
 *   <li>message.occurredAt == event.occurredAt 검증 (CreateTime 파티셔닝 전제)</li>
 * </ol>
 */
@MessageQueueFlowTest
@SpringBootTest(classes = UserActivityS3SinkPublisherFlowTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(partitions = 1, topics = {"evt.user-activity.s3-ingest.v1"})
@DisplayName("[통합](UserActivityS3) UserActivityS3SinkPublisher EmbeddedKafka 플로우 테스트")
class UserActivityS3SinkPublisherFlowTest {

	// TODO: @Resource UserActivityS3SinkPublisher publisher;
	// TODO: @Resource KafkaMessageQueueProperties kafkaProperties;

	@Test
	@DisplayName("ActivityEvent 발행 → evt.user-activity.s3-ingest.v1 컨슘 → UserActivityS3Event 14개 필드 검증")
	void sink_publishAndConsume_verifyAllS3EventFields() {
		// TODO: given - ActivityEvent 샘플 생성
		ActivityEvent event = new ActivityEvent(
			"evt-flow-1",
			"ui.restaurant.clicked",
			"v1",
			Instant.parse("2026-03-11T10:00:00Z"),
			100L,
			null,
			Map.of(
				"restaurantId", 42L,
				"recommendationId", "rec-flow-001",
				"platform", "IOS",
				"sessionId", "sess-flow-abc",
				"diningType", "DINE_IN",
				"distanceBucket", "NEAR",
				"weatherBucket", "CLEAR"));

		// TODO: when - publisher.sink(event)

		// TODO: then - KafkaConsumer로 메시지 수신 후 QueueMessageEnvelope 역직렬화
		// TODO: payload에서 UserActivityS3Event 추출
		// TODO: 14개 컬럼 전체 검증:
		//   assertThat(s3Event.eventId()).isEqualTo("evt-flow-1")
		//   assertThat(s3Event.eventName()).isEqualTo("ui.restaurant.clicked")
		//   assertThat(s3Event.eventVersion()).isEqualTo("v1")
		//   assertThat(s3Event.occurredAt()).isEqualTo(Instant.parse("2026-03-11T10:00:00Z"))
		//   assertThat(s3Event.diningType()).isEqualTo("DINE_IN")
		//   assertThat(s3Event.distanceBucket()).isEqualTo("NEAR")
		//   assertThat(s3Event.weatherBucket()).isEqualTo("CLEAR")
		//   assertThat(s3Event.memberId()).isEqualTo(100L)
		//   assertThat(s3Event.anonymousId()).isNull()
		//   assertThat(s3Event.sessionId()).isEqualTo("sess-flow-abc")
		//   assertThat(s3Event.restaurantId()).isEqualTo(42L)
		//   assertThat(s3Event.recommendationId()).isEqualTo("rec-flow-001")
		//   assertThat(s3Event.platform()).isEqualTo("IOS")
		//   assertThat(s3Event.createdAt()).isNotNull()
		// TODO: key = "100" (memberId) 검증
	}

	@Configuration
	static class TestConfig {}
}
