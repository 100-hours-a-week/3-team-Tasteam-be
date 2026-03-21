package com.tasteam.domain.chat.stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.chat.config.ChatStreamProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ChatWsBroadcastPublisher {

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final ChatStreamProperties chatStreamProperties;
	private final ChatStreamGroupNameProvider groupNameProvider;

	public boolean publish(ChatStreamPayload payload) {
		ChatWsBroadcastEvent event = ChatWsBroadcastEvent.from(payload, groupNameProvider.consumerName());
		String message;
		try {
			message = objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException ex) {
			log.warn("Failed to serialize chat websocket broadcast event. roomId={}", payload.chatRoomId(), ex);
			return false;
		}

		try {
			stringRedisTemplate.convertAndSend(chatStreamProperties.wsPubSubChannel(), message);
			return true;
		} catch (Exception ex) {
			log.warn("Failed to publish chat websocket broadcast event. channel={}",
				chatStreamProperties.wsPubSubChannel(), ex);
			return false;
		}
	}
}
