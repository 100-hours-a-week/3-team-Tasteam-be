package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Message) TopicExpressions 테스트")
class TopicExpressionsTest {

	@Test
	@DisplayName("SpEL 래퍼 메서드가 TopicNamingPolicy 결과를 반환한다")
	void methods_returnResolvedTopics() {
		TopicNamingPolicy policy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		TopicExpressions expressions = new TopicExpressions(policy);

		assertThat(expressions.groupMemberJoinedMain()).isEqualTo(policy.main(QueueTopic.GROUP_MEMBER_JOINED));
		assertThat(expressions.notificationRequestedMain()).isEqualTo(policy.main(QueueTopic.NOTIFICATION_REQUESTED));
		assertThat(expressions.notificationRequestedDlq()).isEqualTo(policy.dlq(QueueTopic.NOTIFICATION_REQUESTED));
		assertThat(expressions.userActivityMain()).isEqualTo(policy.main(QueueTopic.USER_ACTIVITY));
	}
}
