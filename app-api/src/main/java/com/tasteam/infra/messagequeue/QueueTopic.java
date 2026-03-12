package com.tasteam.infra.messagequeue;

public enum QueueTopic {
	GROUP_MEMBER_JOINED("group.member-joined", "domain.group.member-joined"),
	NOTIFICATION_REQUESTED("notification.dispatch", "evt.notification.v1"),
	USER_ACTIVITY_S3_INGEST("user-activity-s3-ingest", "evt.user-activity.s3-ingest.v1");

	private final String key;
	private final String defaultMainTopic;

	QueueTopic(String key, String defaultMainTopic) {
		this.key = key;
		this.defaultMainTopic = defaultMainTopic;
	}

	public String key() {
		return key;
	}

	public String defaultMainTopic() {
		return defaultMainTopic;
	}
}
