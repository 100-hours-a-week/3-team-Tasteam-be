package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Message) DefaultTopicNamingPolicy 테스트")
class DefaultTopicNamingPolicyTest {

	@Test
	@DisplayName("QueueTopic으로 main/dlq를 일관되게 해석한다")
	void resolve_withQueueTopic_returnsTopicSet() {
		DefaultTopicNamingPolicy policy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());

		TopicSet notification = policy.resolve(QueueTopic.NOTIFICATION_REQUESTED);

		assertThat(notification.main()).isEqualTo("evt.notification.dispatch.v1");
		assertThat(notification.dlq()).isEqualTo("evt.notification.dispatch.v1.dlq");
		assertThat(notification.retry(2)).isEqualTo("evt.notification.dispatch.v1.retry.2");
	}

	@Test
	@DisplayName("도메인 override 설정이 있으면 해당 값을 우선 사용한다")
	void resolve_withOverrides_prioritizesConfiguredValues() {
		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		properties.getNotification().setTopic("evt.notification.custom.v2");
		properties.getNotification().setDlqTopic("evt.notification.custom.v2.dlq.custom");

		DefaultTopicNamingPolicy policy = new DefaultTopicNamingPolicy(properties);

		assertThat(policy.main(QueueTopic.NOTIFICATION_REQUESTED)).isEqualTo("evt.notification.custom.v2");
		assertThat(policy.dlq(QueueTopic.NOTIFICATION_REQUESTED)).isEqualTo("evt.notification.custom.v2.dlq.custom");
	}

	@Test
	@DisplayName("source topic 기반 DLQ 해석은 설정값 또는 suffix 규칙을 따른다")
	void dlq_withSourceTopic_usesConfiguredOrSuffix() {
		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		properties.getUserActivity().setTopic("evt.user.activity.v2");
		properties.getUserActivity().setDlqTopic("evt.user.activity.v2.dead");

		DefaultTopicNamingPolicy policy = new DefaultTopicNamingPolicy(properties);

		assertThat(policy.dlq("evt.user.activity.v2")).isEqualTo("evt.user.activity.v2.dead");
		assertThat(policy.dlq("evt.unknown.v1")).isEqualTo("evt.unknown.v1.dlq");
		assertThat(policy.dlq("evt.unknown.v1.dlq")).isEqualTo("evt.unknown.v1.dlq");
		assertThat(policy.dlq((String)null)).isEqualTo("unknown.dlq");
	}

	@Test
	@DisplayName("retry attempt는 1 이상이어야 한다")
	void retry_withInvalidAttempt_throwsException() {
		TopicSet topicSet = new TopicSet("evt.sample.v1", "evt.sample.v1.dlq");

		assertThatThrownBy(() -> topicSet.retry(0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("1 이상");
	}
}
