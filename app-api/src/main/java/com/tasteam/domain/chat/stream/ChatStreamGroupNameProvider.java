package com.tasteam.domain.chat.stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatStreamGroupNameProvider {
	private final String groupName;
	private final String consumerName;

	public ChatStreamGroupNameProvider(
		@Value("${spring.application.name:app}")
		String appName,
		@Value("${random.uuid}")
		String random) {
		this.groupName = "chat-group-" + appName + "-" + random;
		this.consumerName = this.groupName;
	}

	public String groupName() {
		return groupName;
	}

	public String consumerName() {
		return consumerName;
	}
}
