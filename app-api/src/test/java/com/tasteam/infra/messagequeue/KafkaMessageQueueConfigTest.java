package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Message) KafkaMessageQueueConfig 조건부 빈 테스트")
class KafkaMessageQueueConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(KafkaMessageQueueConfig.class);

	@Test
	@DisplayName("MQ 비활성이면 Kafka 관련 빈을 만들지 않는다")
	void whenMqDisabled_thenKafkaBeansNotCreated() {
		contextRunner
			.withPropertyValues(
				"tasteam.message-queue.enabled=false",
				"tasteam.message-queue.provider=kafka")
			.run(context -> {
				assertThat(context).doesNotHaveBean(KafkaTemplate.class);
				assertThat(context).doesNotHaveBean("messageQueueKafkaProducerFactory");
				assertThat(context).doesNotHaveBean("messageQueueKafkaConsumerFactory");
				assertThat(context).doesNotHaveBean("messageQueueKafkaListenerContainerFactory");
			});
	}

	@Test
	@DisplayName("provider가 kafka가 아니면 Kafka 관련 빈을 만들지 않는다")
	void whenProviderNotKafka_thenKafkaBeansNotCreated() {
		contextRunner
			.withPropertyValues(
				"tasteam.message-queue.enabled=true",
				"tasteam.message-queue.provider=redis-stream")
			.run(context -> {
				assertThat(context).doesNotHaveBean(KafkaTemplate.class);
				assertThat(context).doesNotHaveBean("messageQueueKafkaProducerFactory");
				assertThat(context).doesNotHaveBean("messageQueueKafkaConsumerFactory");
				assertThat(context).doesNotHaveBean("messageQueueKafkaListenerContainerFactory");
			});
	}

	@Test
	@DisplayName("MQ 활성화 + provider=kafka이면 Kafka 공통 빈을 만든다")
	void whenMqEnabledAndProviderKafka_thenKafkaBeansCreated() {
		contextRunner
			.withPropertyValues(
				"tasteam.message-queue.enabled=true",
				"tasteam.message-queue.provider=kafka",
				"tasteam.message-queue.default-consumer-group=cg.tasteam.v1",
				"tasteam.message-queue.kafka.bootstrap-servers=localhost:29092",
				"tasteam.message-queue.kafka.client-id=tasteam-test-client",
				"tasteam.message-queue.kafka.producer.acks=1",
				"tasteam.message-queue.kafka.producer.retries=7",
				"tasteam.message-queue.kafka.consumer.max-poll-records=120",
				"tasteam.message-queue.kafka.consumer.concurrency=2",
				"tasteam.message-queue.kafka.consumer.poll-timeout-millis=2100")
			.run(context -> {
				assertThat(context).hasSingleBean(KafkaTemplate.class);
				assertThat(context).hasBean("messageQueueKafkaProducerFactory");
				assertThat(context).hasBean("messageQueueKafkaConsumerFactory");
				assertThat(context).hasBean("messageQueueKafkaListenerContainerFactory");

				DefaultKafkaProducerFactory<?, ?> producerFactory = (DefaultKafkaProducerFactory<?, ?>)context
					.getBean("messageQueueKafkaProducerFactory");
				assertThat(producerFactory.getConfigurationProperties())
					.containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092")
					.containsEntry(ProducerConfig.CLIENT_ID_CONFIG, "tasteam-test-client")
					.containsEntry(ProducerConfig.ACKS_CONFIG, "1")
					.containsEntry(ProducerConfig.RETRIES_CONFIG, 7);

				DefaultKafkaConsumerFactory<?, ?> consumerFactory = (DefaultKafkaConsumerFactory<?, ?>)context
					.getBean("messageQueueKafkaConsumerFactory");
				assertThat(consumerFactory.getConfigurationProperties())
					.containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092")
					.containsEntry(ConsumerConfig.CLIENT_ID_CONFIG, "tasteam-test-client")
					.containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "cg.tasteam.v1")
					.containsEntry(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 120);

				@SuppressWarnings("unchecked") ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory = (ConcurrentKafkaListenerContainerFactory<String, String>)context
					.getBean(
						"messageQueueKafkaListenerContainerFactory");
				assertThat(ReflectionTestUtils.getField(listenerFactory, "concurrency")).isEqualTo(2);
				assertThat(listenerFactory.getContainerProperties().getPollTimeout()).isEqualTo(2100L);
			});
	}
}
