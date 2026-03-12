package com.tasteam.infra.messagequeue.dlq;

public interface DlqTopicNamingPolicy {

	String resolveDlqTopic(String sourceTopic);
}
