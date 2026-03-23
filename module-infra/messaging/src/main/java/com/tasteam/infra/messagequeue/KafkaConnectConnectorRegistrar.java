package com.tasteam.infra.messagequeue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "provider", havingValue = "kafka")
public class KafkaConnectConnectorRegistrar implements ApplicationRunner {

	private static final String CONNECTOR_NAME = "user-activity-s3-sink";
	private static final int MAX_ATTEMPTS = 3;
	private static final long RETRY_DELAY_MS = 10_000L;

	private final KafkaMessageQueueProperties kafkaProps;
	private final RestTemplate restTemplate;

	public KafkaConnectConnectorRegistrar(KafkaMessageQueueProperties kafkaProps) {
		this.kafkaProps = kafkaProps;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5_000);
		factory.setReadTimeout(5_000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!kafkaProps.getConnector().isAutoRegister()) {
			return;
		}
		Thread.ofVirtual()
			.name("kafka-connect-registrar")
			.start(this::registerWithRetry);
	}

	private void registerWithRetry() {
		String connectUrl = kafkaProps.getConnectUrl();
		Map<String, Object> config = buildConnectorConfig();

		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				registerOrUpdate(connectUrl, CONNECTOR_NAME, config);
				return;
			} catch (Exception e) {
				log.warn("Kafka Connect 커넥터 등록 실패 (attempt={}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
				if (attempt < MAX_ATTEMPTS) {
					try {
						Thread.sleep(RETRY_DELAY_MS);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						log.warn("Kafka Connect 커넥터 등록 인터럽트로 중단 — 수동 등록 필요");
						return;
					}
				}
			}
		}
		log.error("Kafka Connect 커넥터 자동 등록 최종 실패 ({}회 시도) — 수동 등록 필요: POST {}/connectors",
			MAX_ATTEMPTS, connectUrl);
	}

	@SuppressWarnings("unchecked")
	private void registerOrUpdate(String connectUrl, String name, Map<String, Object> config) {
		String getUrl = connectUrl + "/connectors/" + name;
		try {
			restTemplate.getForObject(getUrl, Map.class);
			// 이미 존재 → config 업데이트
			restTemplate.put(getUrl + "/config", config);
			log.info("Kafka Connect 커넥터 업데이트 완료: {}", name);
		} catch (HttpClientErrorException.NotFound e) {
			// 미존재 → 신규 등록
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("name", name);
			body.put("config", config);
			try {
				restTemplate.postForObject(connectUrl + "/connectors", body, Map.class);
				log.info("Kafka Connect 커넥터 등록 완료: {}", name);
			} catch (HttpClientErrorException.Conflict conflict) {
				// 조회-생성 사이 레이스로 이미 생성된 경우는 정상 흐름으로 보고 config만 맞춘다.
				restTemplate.put(getUrl + "/config", config);
				log.info("Kafka Connect 커넥터가 이미 존재해 config 업데이트로 전환: {}", name);
			}
		}
	}

	private Map<String, Object> buildConnectorConfig() {
		KafkaMessageQueueProperties.UserActivityS3IngestProperties ingest = kafkaProps.getUserActivityS3Ingest();
		KafkaMessageQueueProperties.UserActivityS3SinkConnectorProperties s3 = kafkaProps.getConnector()
			.getUserActivityS3Sink();

		Map<String, Object> config = new LinkedHashMap<>();
		config.put("connector.class", "io.confluent.connect.s3.S3SinkConnector");
		config.put("tasks.max", String.valueOf(s3.getTasksMax()));
		config.put("topics", ingest.getTopic());
		config.put("topics.dir", "");
		config.put("s3.bucket.name", s3.getBucket());
		config.put("s3.region", s3.getRegion());
		config.put("aws.access.key.id", s3.getAccessKeyId());
		config.put("aws.secret.access.key", s3.getSecretAccessKey());
		config.put("format.class", "io.confluent.connect.s3.format.json.JsonFormat");
		config.put("s3.compression.type", "gzip");
		config.put("flush.size", String.valueOf(s3.getFlushSize()));
		config.put("rotate.interval.ms", String.valueOf(s3.getRotateIntervalMs()));
		config.put("rotate.schedule.interval.ms", String.valueOf(s3.getRotateScheduleIntervalMs()));
		config.put("partitioner.class", "io.confluent.connect.storage.partitioner.TimeBasedPartitioner");
		config.put("partition.duration.ms", "86400000");
		config.put("timestamp.extractor", "Record");
		config.put("path.format", "'raw/events/dt='YYYY-MM-dd");
		config.put("timezone", "Asia/Seoul");
		config.put("locale", "ko_KR");
		config.put("transforms", "unwrapEnvelope");
		config.put("transforms.unwrapEnvelope.type",
			"org.apache.kafka.connect.transforms.ExtractField$Value");
		config.put("transforms.unwrapEnvelope.field", "payload");
		config.put("errors.tolerance", "all");
		config.put("errors.deadletterqueue.topic.name", ingest.getDlqTopic());
		config.put("errors.deadletterqueue.topic.replication.factor", "1");
		config.put("consumer.override.group.id", "cg.user-activity.s3-ingest.v1");
		config.put("schemas.enable", "false");
		config.put("storage.class", "io.confluent.connect.s3.storage.S3Storage");

		if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
			config.put("store.url", s3.getEndpoint());
			config.put("s3.endpoint", s3.getEndpoint());
			config.put("s3.path.style.access", String.valueOf(s3.isPathStyleAccess()));
		}

		return config;
	}
}
