package com.tasteam.infra.messagequeue.dlq;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.KafkaMessageQueueProperties;

@UnitTest
@DisplayName("[유닛](Message) DefaultDlqTopicNamingPolicy 테스트")
class DefaultDlqTopicNamingPolicyTest {

	@Test
	@DisplayName("도메인별 DLQ 설정이 있으면 해당 토픽을 반환한다")
	void resolveDlqTopic_whenConfiguredDomainTopic_returnsConfiguredDlqTopic() {
		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		properties.getNotification().setTopic("evt.notification.dispatch.v1");
		properties.getNotification().setDlqTopic("evt.notification.dispatch.v1.dlq.custom");

		DefaultDlqTopicNamingPolicy policy = new DefaultDlqTopicNamingPolicy(properties);

		assertThat(policy.resolveDlqTopic("evt.notification.dispatch.v1"))
			.isEqualTo("evt.notification.dispatch.v1.dlq.custom");
	}

	@Test
	@DisplayName("도메인 설정이 없으면 기본 규칙(source + .dlq)을 사용한다")
	void resolveDlqTopic_whenNoConfiguredTopic_returnsDefaultSuffix() {
		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		DefaultDlqTopicNamingPolicy policy = new DefaultDlqTopicNamingPolicy(properties);

		assertThat(policy.resolveDlqTopic("evt.unknown.topic.v1"))
			.isEqualTo("evt.unknown.topic.v1.dlq");
	}

	@Test
	@DisplayName("입력 토픽이 이미 .dlq로 끝나면 그대로 반환한다")
	void resolveDlqTopic_whenAlreadyDlq_returnsSameTopic() {
		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		DefaultDlqTopicNamingPolicy policy = new DefaultDlqTopicNamingPolicy(properties);

		assertThat(policy.resolveDlqTopic("evt.notification.dispatch.v1.dlq"))
			.isEqualTo("evt.notification.dispatch.v1.dlq");
	}
}
