package com.tasteam.infra.messagequeue;

import java.util.EnumMap;
import java.util.Map;

public class DefaultTopicNamingPolicy implements TopicNamingPolicy {

	private static final String DLQ_SUFFIX = ".dlq";
	private final Map<QueueTopic, TopicSet> topicSets;
	private final Map<String, String> dlqByMainTopic;

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

		TopicSet userActivity = new TopicSet(
			emptyToDefault(properties.getUserActivity().getTopic(), QueueTopic.USER_ACTIVITY.defaultMainTopic()),
			emptyToDefault(properties.getUserActivity().getDlqTopic(),
				emptyToDefault(properties.getUserActivity().getTopic(), QueueTopic.USER_ACTIVITY.defaultMainTopic())
					+ DLQ_SUFFIX));
		sets.put(QueueTopic.USER_ACTIVITY, userActivity);

		TopicSet analytics = new TopicSet(
			emptyToDefault(properties.getAnalyticsEventLog().getTopic(),
				QueueTopic.ANALYTICS_EVENT_LOG.defaultMainTopic()),
			emptyToDefault(properties.getAnalyticsEventLog().getDlqTopic(),
				emptyToDefault(properties.getAnalyticsEventLog().getTopic(),
					QueueTopic.ANALYTICS_EVENT_LOG.defaultMainTopic())
					+ DLQ_SUFFIX));
		sets.put(QueueTopic.ANALYTICS_EVENT_LOG, analytics);

		topicSets = Map.copyOf(sets);
		dlqByMainTopic = topicSets.values().stream()
			.collect(java.util.stream.Collectors.toUnmodifiableMap(TopicSet::main, TopicSet::dlq));
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

	private String emptyToDefault(String value, String defaultValue) {
		return (value == null || value.isBlank()) ? defaultValue : value;
	}
}
