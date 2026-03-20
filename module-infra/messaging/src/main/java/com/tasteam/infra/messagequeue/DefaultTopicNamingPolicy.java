package com.tasteam.infra.messagequeue;

import java.util.EnumMap;
import java.util.Map;

public class DefaultTopicNamingPolicy implements TopicNamingPolicy {

	private static final String DLQ_SUFFIX = ".dlq";
	private final Map<QueueTopic, TopicSet> topicSets;
	private final Map<String, String> dlqByMainTopic;
	private final Map<QueueTopic, String> consumerGroups;

	public DefaultTopicNamingPolicy(KafkaMessageQueueProperties properties) {
		Map<QueueTopic, TopicSet> sets = new EnumMap<>(QueueTopic.class);
		for (QueueTopic topic : QueueTopic.values()) {
			sets.put(topic, new TopicSet(topic.defaultMainTopic(), topic.defaultMainTopic() + DLQ_SUFFIX));
		}

		TopicSet notification = new TopicSet(
			emptyToDefault(properties.getNotification().getTopic(),
				QueueTopic.NOTIFICATION_REQUESTED.defaultMainTopic()),
			emptyToDefault(properties.getNotification().getDlqTopic(),
				emptyToDefault(properties.getNotification().getTopic(),
					QueueTopic.NOTIFICATION_REQUESTED.defaultMainTopic())
					+ DLQ_SUFFIX));
		sets.put(QueueTopic.NOTIFICATION_REQUESTED, notification);

		TopicSet userActivityS3Ingest = new TopicSet(
			emptyToDefault(properties.getUserActivityS3Ingest().getTopic(),
				QueueTopic.USER_ACTIVITY_S3_INGEST.defaultMainTopic()),
			emptyToDefault(properties.getUserActivityS3Ingest().getDlqTopic(),
				emptyToDefault(properties.getUserActivityS3Ingest().getTopic(),
					QueueTopic.USER_ACTIVITY_S3_INGEST.defaultMainTopic())
					+ DLQ_SUFFIX));
		sets.put(QueueTopic.USER_ACTIVITY_S3_INGEST, userActivityS3Ingest);

		topicSets = Map.copyOf(sets);
		dlqByMainTopic = topicSets.values().stream()
			.collect(java.util.stream.Collectors.toUnmodifiableMap(TopicSet::main, TopicSet::dlq));

		Map<QueueTopic, String> groups = new EnumMap<>(QueueTopic.class);
		groups.put(QueueTopic.GROUP_MEMBER_JOINED, "cg.group.member-joined.v1");
		groups.put(QueueTopic.NOTIFICATION_REQUESTED,
			emptyToDefault(properties.getNotification().getConsumerGroup(), "cg.notification.processor.v1"));
		consumerGroups = Map.copyOf(groups);
	}

	@Override
	public TopicSet resolve(QueueTopic topic) {
		TopicSet topicSet = topicSets.get(topic);
		if (topicSet == null) {
			throw new IllegalArgumentException("지원하지 않는 QueueTopic입니다: " + topic);
		}
		return topicSet;
	}

	@Override
	public String dlq(String sourceTopic) {
		if (sourceTopic == null || sourceTopic.isBlank()) {
			return "unknown" + DLQ_SUFFIX;
		}
		String configuredDlqTopic = dlqByMainTopic.get(sourceTopic);
		if (configuredDlqTopic != null && !configuredDlqTopic.isBlank()) {
			return configuredDlqTopic;
		}
		if (sourceTopic.endsWith(DLQ_SUFFIX)) {
			return sourceTopic;
		}
		return sourceTopic + DLQ_SUFFIX;
	}

	@Override
	public String consumerGroup(QueueTopic topic) {
		String group = consumerGroups.get(topic);
		if (group == null || group.isBlank()) {
			throw new IllegalArgumentException("지원하지 않는 QueueTopic consumerGroup입니다: " + topic);
		}
		return group;
	}

	private String emptyToDefault(String value, String defaultValue) {
		return (value == null || value.isBlank()) ? defaultValue : value;
	}
}
