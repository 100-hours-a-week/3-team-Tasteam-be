package com.tasteam.infra.messagequeue;

public final class MessageQueueTopics {

	public static final String GROUP_MEMBER_JOINED = "domain.group.member-joined";
	public static final String USER_ACTIVITY = "domain.user.activity";
	public static final String NOTIFICATION_REQUESTED = "evt.notification.v1";
	public static final String NOTIFICATION_REQUESTED_DLQ = "evt.notification.v1.dlq";

	private MessageQueueTopics() {}
}
