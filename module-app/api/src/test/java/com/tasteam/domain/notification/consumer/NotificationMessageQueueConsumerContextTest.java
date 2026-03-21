package com.tasteam.domain.notification.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.DefaultTopicNamingPolicy;
import com.tasteam.infra.messagequeue.KafkaMessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueConsumer;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.TopicNamingPolicy;

@UnitTest
@DisplayName("[유닛](Notification) NotificationMessageQueueConsumer 조건부 등록 테스트")
class NotificationMessageQueueConsumerContextTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(MessageQueueConsumer.class, () -> mock(MessageQueueConsumer.class))
		.withBean(MessageQueueProperties.class, MessageQueueProperties::new)
		.withBean(TopicNamingPolicy.class, () -> new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties()))
		.withBean(NotificationMessageProcessor.class, () -> mock(NotificationMessageProcessor.class))
		.withBean(NotificationDlqPublisher.class, () -> mock(NotificationDlqPublisher.class))
		.withBean(ObjectMapper.class, ObjectMapper::new)
		.withUserConfiguration(NotificationMessageQueueConsumer.class);

	@Test
	@DisplayName("MQ 활성화 + provider=kafka이면 알림 MQ consumer 빈을 등록한다")
	void whenMqEnabledAndProviderKafka_thenConsumerBeanCreated() {
		contextRunner
			.withPropertyValues(
				"tasteam.message-queue.enabled=true",
				"tasteam.message-queue.provider=kafka")
			.run(context -> assertThat(context).hasSingleBean(NotificationMessageQueueConsumer.class));
	}

	@Test
	@DisplayName("MQ 비활성이면 알림 MQ consumer 빈을 등록하지 않는다")
	void whenMqDisabled_thenConsumerBeanNotCreated() {
		contextRunner
			.withPropertyValues(
				"tasteam.message-queue.enabled=false",
				"tasteam.message-queue.provider=kafka")
			.run(context -> assertThat(context).doesNotHaveBean(NotificationMessageQueueConsumer.class));
	}
}
