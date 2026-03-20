package com.tasteam.infra.messagequeue;

public enum MessageQueueProviderType {

	NONE("none"),
	REDIS_STREAM("redis-stream"),
	KAFKA("kafka");

	private final String value;

	MessageQueueProviderType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static MessageQueueProviderType from(String raw) {
		if (raw == null || raw.isBlank()) {
			return NONE;
		}
		String normalized = raw.trim().toLowerCase()
			.replace("_", "-")
			.replace(" ", "-");
		return switch (normalized) {
			case "none" -> NONE;
			case "redis-stream", "redisstream", "redis" -> REDIS_STREAM;
			case "kafka" -> KAFKA;
			default -> throw new IllegalArgumentException("지원하지 않는 메시지큐 provider: " + raw);
		};
	}
}
