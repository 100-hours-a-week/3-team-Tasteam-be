package com.tasteam.infra.messagequeue.dlq;

import java.util.HashMap;
import java.util.Map;

import com.tasteam.infra.messagequeue.KafkaMessageQueueProperties;

public class DefaultDlqTopicNamingPolicy implements DlqTopicNamingPolicy {

	private static final String DLQ_SUFFIX = ".dlq";
	private final Map<String, String> domainDlqTopicBySourceTopic = new HashMap<>();

	public DefaultDlqTopicNamingPolicy(KafkaMessageQueueProperties properties) {
		put(properties.getAnalyticsEventLog().getTopic(), properties.getAnalyticsEventLog().getDlqTopic());
		put(properties.getNotification().getTopic(), properties.getNotification().getDlqTopic());
		put(properties.getUserActivity().getTopic(), properties.getUserActivity().getDlqTopic());
	}

	@Override
	public String resolveDlqTopic(String sourceTopic) {
		if (sourceTopic == null || sourceTopic.isBlank()) {
			return "unknown" + DLQ_SUFFIX;
		}
		String configuredDlqTopic = domainDlqTopicBySourceTopic.get(sourceTopic);
		if (configuredDlqTopic != null && !configuredDlqTopic.isBlank()) {
			return configuredDlqTopic;
		}
		if (sourceTopic.endsWith(DLQ_SUFFIX)) {
			return sourceTopic;
		}
		return sourceTopic + DLQ_SUFFIX;
	}

	private void put(String sourceTopic, String dlqTopic) {
		if (sourceTopic == null || sourceTopic.isBlank() || dlqTopic == null || dlqTopic.isBlank()) {
			return;
		}
		domainDlqTopicBySourceTopic.put(sourceTopic, dlqTopic);
	}
}
