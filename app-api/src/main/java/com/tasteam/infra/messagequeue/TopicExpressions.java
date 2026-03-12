package com.tasteam.infra.messagequeue;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component("topics")
@RequiredArgsConstructor
public class TopicExpressions {

	private final TopicNamingPolicy topicNamingPolicy;

	public String groupMemberJoinedMain() {
		return topicNamingPolicy.main(QueueTopic.GROUP_MEMBER_JOINED);
	}

	public String notificationRequestedMain() {
		return topicNamingPolicy.main(QueueTopic.NOTIFICATION_REQUESTED);
	}

	public String notificationRequestedDlq() {
		return topicNamingPolicy.dlq(QueueTopic.NOTIFICATION_REQUESTED);
	}
}
