package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Message) DefaultQueueEventSubscriber 테스트")
class DefaultQueueEventSubscriberTest {

	@Test
	@DisplayName("QueueTopic만으로 구독을 등록하고 subscription을 반환한다")
	void subscribe_registersWithPolicyResolvedMetadata() {
		MessageBrokerReceiver brokerReceiver = mock(MessageBrokerReceiver.class);
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		DefaultQueueEventSubscriber subscriber = new DefaultQueueEventSubscriber(brokerReceiver, topicNamingPolicy);

		MessageQueueSubscription subscription = subscriber.subscribe(QueueTopic.USER_ACTIVITY, message -> {});

		ArgumentCaptor<MessageQueueSubscription> captor = ArgumentCaptor.forClass(MessageQueueSubscription.class);
		verify(brokerReceiver).subscribe(captor.capture(), any(QueueMessageHandler.class));
		assertThat(captor.getValue().topic()).isEqualTo(topicNamingPolicy.main(QueueTopic.USER_ACTIVITY));
		assertThat(captor.getValue().consumerGroup())
			.isEqualTo(topicNamingPolicy.consumerGroup(QueueTopic.USER_ACTIVITY));
		assertThat(subscription).isEqualTo(captor.getValue());
	}
}
