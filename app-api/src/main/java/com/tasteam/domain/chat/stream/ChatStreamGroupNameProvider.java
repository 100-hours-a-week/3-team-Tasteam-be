package com.tasteam.domain.chat.stream;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatStreamGroupNameProvider {
	private final String groupName;
	private final String consumerName;

	public ChatStreamGroupNameProvider(
		@Value("${spring.application.name:app}")
		String appName) {
		this.groupName = "chat-group-" + sanitize(appName);
		this.consumerName = resolveConsumerName();
	}

	public String groupName() {
		return groupName;
	}

	public String consumerName() {
		return consumerName;
	}

	private String resolveConsumerName() {
		String podName = System.getenv("POD_NAME");
		if (StringUtils.hasText(podName)) {
			return sanitize(podName);
		}

		String hostname = resolveHostname();
		Long pid = resolvePid();
		if (hostname != null && pid != null) {
			return sanitize(hostname + "-" + pid);
		}

		String fallbackHost = hostname == null ? "unknown-host" : hostname;
		return sanitize(fallbackHost + "-" + UUID.randomUUID());
	}

	private String resolveHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
			return null;
		}
	}

	private Long resolvePid() {
		try {
			return ProcessHandle.current().pid();
		} catch (Exception ex) {
			try {
				return Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
			} catch (Exception ignored) {
				return null;
			}
		}
	}

	private String sanitize(String value) {
		if (!StringUtils.hasText(value)) {
			return "app";
		}
		return value.replaceAll("[^a-zA-Z0-9._:-]", "_");
	}
}
