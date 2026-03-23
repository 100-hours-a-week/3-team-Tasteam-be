package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Message) KafkaMessageQueueProperties 바인딩 테스트")
class KafkaMessageQueuePropertiesBindingTest {

	@Test
	@DisplayName("명시된 설정값을 Kafka 공통/도메인 프로퍼티로 바인딩한다")
	void bind_withExplicitValues_mapsAllFields() {
		// given
		Map<String, String> source = Map.ofEntries(
			Map.entry("tasteam.message-queue.kafka.bootstrap-servers", "kafka-1:9092,kafka-2:9092"),
			Map.entry("tasteam.message-queue.kafka.client-id", "tasteam-app"),
			Map.entry("tasteam.message-queue.kafka.producer.acks", "1"),
			Map.entry("tasteam.message-queue.kafka.producer.retries", "5"),
			Map.entry("tasteam.message-queue.kafka.producer.batch-size", "32768"),
			Map.entry("tasteam.message-queue.kafka.producer.linger-ms", "20"),
			Map.entry("tasteam.message-queue.kafka.producer.send-timeout-millis", "4500"),
			Map.entry("tasteam.message-queue.kafka.consumer.concurrency", "3"),
			Map.entry("tasteam.message-queue.kafka.consumer.max-poll-records", "100"),
			Map.entry("tasteam.message-queue.kafka.consumer.poll-timeout-millis", "2500"),
			Map.entry("tasteam.message-queue.kafka.consumer.retry.max-attempts", "4"),
			Map.entry("tasteam.message-queue.kafka.consumer.retry.backoff-millis", "900"),
			Map.entry("tasteam.message-queue.kafka.notification.topic", "evt.notification.custom.v1"),
			Map.entry("tasteam.message-queue.kafka.notification.consumer-group", "cg.notification.custom.v1"),
			Map.entry("tasteam.message-queue.kafka.notification.dlq-topic", "evt.notification.custom.v1.dlq"),
			Map.entry("tasteam.message-queue.kafka.user-activity-s3-ingest.topic",
				"evt.user-activity.s3-ingest.custom.v1"),
			Map.entry("tasteam.message-queue.kafka.user-activity-s3-ingest.dlq-topic",
				"evt.user-activity.s3-ingest.custom.v1.dlq"));
		Binder binder = new Binder(new MapConfigurationPropertySource(source));

		// when
		KafkaMessageQueueProperties properties = binder.bind(
			"tasteam.message-queue.kafka",
			Bindable.of(KafkaMessageQueueProperties.class)).orElseGet(KafkaMessageQueueProperties::new);

		// then
		assertThat(properties.getBootstrapServers()).isEqualTo("kafka-1:9092,kafka-2:9092");
		assertThat(properties.getClientId()).isEqualTo("tasteam-app");
		assertThat(properties.getProducer().getAcks()).isEqualTo("1");
		assertThat(properties.getProducer().getRetries()).isEqualTo(5);
		assertThat(properties.getProducer().getBatchSize()).isEqualTo(32768);
		assertThat(properties.getProducer().getLingerMs()).isEqualTo(20);
		assertThat(properties.getProducer().getSendTimeoutMillis()).isEqualTo(4500L);
		assertThat(properties.getConsumer().getConcurrency()).isEqualTo(3);
		assertThat(properties.getConsumer().getMaxPollRecords()).isEqualTo(100);
		assertThat(properties.getConsumer().getPollTimeoutMillis()).isEqualTo(2500L);
		assertThat(properties.getConsumer().getRetry().getMaxAttempts()).isEqualTo(4);
		assertThat(properties.getConsumer().getRetry().getBackoffMillis()).isEqualTo(900L);
		assertThat(properties.getNotification().getTopic()).isEqualTo("evt.notification.custom.v1");
		assertThat(properties.getNotification().getConsumerGroup()).isEqualTo("cg.notification.custom.v1");
		assertThat(properties.getNotification().getDlqTopic()).isEqualTo("evt.notification.custom.v1.dlq");
		assertThat(properties.getUserActivityS3Ingest().getTopic()).isEqualTo("evt.user-activity.s3-ingest.custom.v1");
		assertThat(properties.getUserActivityS3Ingest().getDlqTopic())
			.isEqualTo("evt.user-activity.s3-ingest.custom.v1.dlq");
	}

}
