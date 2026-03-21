package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.MessageQueueFlowTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.infra.messagequeue.serialization.JsonQueueMessageSerializer;
import com.tasteam.infra.messagequeue.serialization.QueueMessageEnvelope;
import com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer;

import jakarta.annotation.Resource;

/**
 * {@link UserActivityS3SinkPublisher} end-to-end 플로우 테스트.
 *
 * <p>Kafka 브로커 의존성이 있어 로컬 Kafka 없이 {@code @EmbeddedKafka}로 검증한다.
 */
@MessageQueueFlowTest
@SpringBootTest(classes = UserActivityS3SinkPublisherFlowTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(partitions = 1, topics = {"evt.user-activity.s3-ingest.v1"})
@DisplayName("[통합](UserActivityS3) UserActivityS3SinkPublisher EmbeddedKafka 플로우 테스트")
class UserActivityS3SinkPublisherFlowTest {

	private static final Duration RECORD_POLL_TIMEOUT = Duration.ofSeconds(10);

	@Resource
	private UserActivityS3SinkPublisher publisher;

	@Resource
	private KafkaMessageQueueProperties kafkaProperties;

	@Resource
	private QueueMessageSerializer queueMessageSerializer;

	@Resource
	private ObjectMapper objectMapper;

	@Resource
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	private Consumer<String, String> consumer;

	@BeforeEach
	void setUpConsumer() {
		Map<String, Object> consumerConfigs = new HashMap<>();
		consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
		consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, "cg.user-activity-s3-flow-test");
		consumerConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerConfigs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerConfigs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(
			consumerConfigs);
		consumer = consumerFactory.createConsumer();
		embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, kafkaProperties.getUserActivityS3Ingest().getTopic());
	}

	@AfterEach
	void tearDownConsumer() {
		if (consumer != null) {
			consumer.close();
		}
	}

	@Test
	@DisplayName("ActivityEvent 발행 → evt.user-activity.s3-ingest.v1 컨슘 → UserActivityS3Event 14개 필드 검증")
	void sink_publishAndConsume_verifyAllS3EventFields() throws Exception {
		// given
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

		// when
		publisher.sink(event);
		ConsumerRecord<String, String> record = pollSingleRecord(kafkaProperties.getUserActivityS3Ingest().getTopic());
		QueueMessageEnvelope envelope = objectMapper.readValue(record.value(), QueueMessageEnvelope.class);
		QueueMessage message = queueMessageSerializer.deserialize(record.value());
		UserActivityS3Event s3Event = objectMapper.treeToValue(envelope.payload(), UserActivityS3Event.class);

		// then
		assertThat(record.topic()).isEqualTo(kafkaProperties.getUserActivityS3Ingest().getTopic());
		assertThat(record.key()).isEqualTo("100");
		assertThat(envelope.topic()).isEqualTo(kafkaProperties.getUserActivityS3Ingest().getTopic());
		assertThat(envelope.key()).isEqualTo("100");
		assertThat(envelope.messageId()).isEqualTo("evt-flow-1");

		assertThat(message.topic()).isEqualTo(kafkaProperties.getUserActivityS3Ingest().getTopic());
		assertThat(message.key()).isEqualTo("100");
		assertThat(message.messageId()).isEqualTo("evt-flow-1");
		assertThat(message.occurredAt()).isEqualTo(event.occurredAt());
		assertThat(message.headers())
			.containsEntry("eventType", "UserActivityS3Event")
			.containsEntry("eventName", "ui.restaurant.clicked")
			.containsEntry("schemaVersion", "v1");

		assertThat(s3Event.eventId()).isEqualTo("evt-flow-1");
		assertThat(s3Event.eventName()).isEqualTo("ui.restaurant.clicked");
		assertThat(s3Event.eventVersion()).isEqualTo("v1");
		assertThat(s3Event.occurredAt()).isEqualTo(Instant.parse("2026-03-11T10:00:00Z"));
		assertThat(s3Event.diningType()).isEqualTo("DINE_IN");
		assertThat(s3Event.distanceBucket()).isEqualTo("NEAR");
		assertThat(s3Event.weatherBucket()).isEqualTo("CLEAR");
		assertThat(s3Event.memberId()).isEqualTo(100L);
		assertThat(s3Event.anonymousId()).isNull();
		assertThat(s3Event.sessionId()).isEqualTo("sess-flow-abc");
		assertThat(s3Event.restaurantId()).isEqualTo(42L);
		assertThat(s3Event.recommendationId()).isEqualTo("rec-flow-001");
		assertThat(s3Event.platform()).isEqualTo("IOS");
		assertThat(s3Event.createdAt()).isNotNull();
	}

	private ConsumerRecord<String, String> pollSingleRecord(String topic) {
		long deadlineNanos = System.nanoTime() + RECORD_POLL_TIMEOUT.toNanos();
		while (System.nanoTime() < deadlineNanos) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
			for (ConsumerRecord<String, String> record : records.records(topic)) {
				return record;
			}
		}
		fail("Kafka 메시지를 수신하지 못했습니다. topic=" + topic);
		throw new IllegalStateException("unreachable");
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return JsonMapper.builder().findAndAddModules().build();
		}

		@Bean
		MessageQueueProperties messageQueueProperties() {
			MessageQueueProperties properties = new MessageQueueProperties();
			properties.setEnabled(true);
			properties.setProvider(MessageQueueProviderType.KAFKA.value());
			properties.setDefaultConsumerGroup("cg.user-activity-s3-flow");
			return properties;
		}

		@Bean
		KafkaMessageQueueProperties kafkaMessageQueueProperties(EmbeddedKafkaBroker embeddedKafkaBroker) {
			KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
			properties.setBootstrapServers(embeddedKafkaBroker.getBrokersAsString());
			properties.setClientId("tasteam-user-activity-s3-flow-test");
			properties.getProducer().setSendTimeoutMillis(10_000L);
			return properties;
		}

		@Bean
		QueueMessageSerializer queueMessageSerializer(ObjectMapper objectMapper) {
			return new JsonQueueMessageSerializer(objectMapper);
		}

		@Bean
		TopicNamingPolicy topicNamingPolicy(KafkaMessageQueueProperties kafkaProperties) {
			return new DefaultTopicNamingPolicy(kafkaProperties);
		}

		@Bean
		ProducerFactory<String, String> producerFactory(KafkaMessageQueueProperties kafkaProperties) {
			Map<String, Object> producerConfigs = new HashMap<>();
			producerConfigs.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
				kafkaProperties.getBootstrapServers());
			producerConfigs.put(org.apache.kafka.clients.producer.ProducerConfig.CLIENT_ID_CONFIG,
				kafkaProperties.getClientId());
			producerConfigs.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG,
				kafkaProperties.getProducer().getAcks());
			producerConfigs.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG,
				kafkaProperties.getProducer().getRetries());
			producerConfigs.put(org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG,
				kafkaProperties.getProducer().getBatchSize());
			producerConfigs.put(org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG,
				kafkaProperties.getProducer().getLingerMs());
			producerConfigs.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class);
			producerConfigs.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class);
			return new DefaultKafkaProducerFactory<>(producerConfigs);
		}

		@Bean
		KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
			return new KafkaTemplate<>(producerFactory);
		}

		@Bean
		KafkaPublishSupport kafkaPublishSupport(
			KafkaTemplate<String, String> kafkaTemplate,
			KafkaMessageQueueProperties kafkaProperties,
			QueueMessageSerializer queueMessageSerializer) {
			return new KafkaPublishSupport(kafkaTemplate, kafkaProperties, queueMessageSerializer);
		}

		@Bean
		MessageQueueProducer messageQueueProducer(KafkaPublishSupport kafkaPublishSupport) {
			return kafkaPublishSupport::publish;
		}

		@Bean
		UserActivityS3SinkPublisher userActivityS3SinkPublisher(
			MessageQueueProducer messageQueueProducer,
			MessageQueueProperties messageQueueProperties,
			TopicNamingPolicy topicNamingPolicy,
			ObjectMapper objectMapper) {
			return new UserActivityS3SinkPublisher(
				messageQueueProducer,
				messageQueueProperties,
				topicNamingPolicy,
				objectMapper);
		}
	}
}
