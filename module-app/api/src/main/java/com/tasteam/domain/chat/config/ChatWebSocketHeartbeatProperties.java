package com.tasteam.domain.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.websocket.heartbeat")
public record ChatWebSocketHeartbeatProperties(
	boolean enabled,
	long serverToClient,
	long clientToServer) {
}
