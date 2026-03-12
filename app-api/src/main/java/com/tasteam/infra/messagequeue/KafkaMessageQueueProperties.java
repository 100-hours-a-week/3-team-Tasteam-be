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
	private AnalyticsEventLogProperties analyticsEventLog = new AnalyticsEventLogProperties();
	private NotificationProperties notification = new NotificationProperties();
	private UserActivityProperties userActivity = new UserActivityProperties();

	@Getter
	@Setter
	public static class ProducerProperties {
		private String acks = "all";
		private int retries = 3;
		private int batchSize = 16384;
		private int lingerMs = 5;
		private long sendTimeoutMillis = 5000L;
	}

	@Getter
	@Setter
	public static class ConsumerProperties {
		private int concurrency = 1;
		private int maxPollRecords = 500;
		private long pollTimeoutMillis = 1000L;
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
	public static class AnalyticsEventLogProperties {
		private String topic = "evt.analytics.event-log.v1";
		private String consumerGroup = "cg.analytics.event-log.v1";
		private String dlqTopic = "evt.analytics.event-log.v1.dlq";
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
	public static class UserActivityProperties {
		private String topic = "evt.user-activity.ingest.v1";
		private String consumerGroup = "cg.user-activity.ingest.v1";
		private String dlqTopic = "evt.user-activity.ingest.v1.dlq";
	}
}
