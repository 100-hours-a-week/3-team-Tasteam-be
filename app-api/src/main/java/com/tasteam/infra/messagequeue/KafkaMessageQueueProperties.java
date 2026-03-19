package com.tasteam.infra.messagequeue;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.message-queue.kafka")
public class KafkaMessageQueueProperties {

	private String bootstrapServers = "localhost:29092";
	private String connectUrl = "http://localhost:8083";
	private String clientId = "tasteam-api";
	private ProducerProperties producer = new ProducerProperties();
	private ConsumerProperties consumer = new ConsumerProperties();
	private NotificationProperties notification = new NotificationProperties();
	private UserActivityS3IngestProperties userActivityS3Ingest = new UserActivityS3IngestProperties();
	private ConnectorProperties connector = new ConnectorProperties();

	@Getter
	@Setter
	public static class ProducerProperties {
		private String acks = "all";
		private int retries = 3;
		private int batchSize = 16384;
		private int lingerMs = 5;
		private long sendTimeoutMillis = 5000L;
		private String transactionIdPrefix = "tasteam-tx-";
	}

	@Getter
	@Setter
	public static class ConsumerProperties {
		private int concurrency = 1;
		private int maxPollRecords = 50;
		private long pollTimeoutMillis = 1000L;
		private int sessionTimeoutMs = 30000;
		private int heartbeatIntervalMs = 10000;
		private int maxPollIntervalMs = 300000;
		private int fetchMinBytes = 1024;
		private int fetchMaxWaitMs = 500;
		private String partitionAssignmentStrategy = "org.apache.kafka.clients.consumer.CooperativeStickyAssignor";
		private RetryProperties retry = new RetryProperties();
	}

	@Getter
	@Setter
	public static class RetryProperties {
		private int maxAttempts = 3;
		private long backoffMillis = 1000L;
	}

	@Getter
	@Setter
	public static class NotificationProperties {
		private String topic = "evt.notification.dispatch.v1";
		private String consumerGroup = "cg.notification.processor.v1";
		private String dlqTopic = "evt.notification.dispatch.v1.dlq";
	}

	@Getter
	@Setter
	public static class UserActivityS3IngestProperties {
		private String topic = "evt.user-activity.s3-ingest.v1";
		private String dlqTopic = "evt.user-activity.s3-ingest.v1.dlq";
	}

	@Getter
	@Setter
	public static class ConnectorProperties {
		private boolean autoRegister = false;
		private UserActivityS3SinkConnectorProperties userActivityS3Sink = new UserActivityS3SinkConnectorProperties();
	}

	@Getter
	@Setter
	public static class UserActivityS3SinkConnectorProperties {
		private String bucket;
		private String region = "ap-northeast-2";
		/** MinIO/LocalStack용 엔드포인트. 비어있으면 AWS S3 직접 사용. */
		private String endpoint;
		private boolean pathStyleAccess = false;
		private String accessKeyId;
		private String secretAccessKey;
	}
}
