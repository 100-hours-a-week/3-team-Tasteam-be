package com.tasteam.domain.chat.stream;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.stream.ws-pubsub-broadcast-enabled", havingValue = "true")
public class ChatWsBroadcastSubscriber implements MessageListener {

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String raw = new String(message.getBody(), StandardCharsets.UTF_8);
		try {
			ChatWsBroadcastEvent event = objectMapper.readValue(raw, ChatWsBroadcastEvent.class);
			Long chatRoomId = event.resolveChatRoomId();
			if (chatRoomId == null) {
				log.warn("Ignore invalid chat websocket broadcast event. reason=missing_chat_room_id");
				return;
			}
			ChatMessageItemResponse itemResponse = event.toPayload().toMessageItem();
			messagingTemplate.convertAndSend("/topic/chat-rooms/" + chatRoomId, itemResponse);
		} catch (Exception ex) {
			log.warn("Failed to handle chat websocket broadcast event", ex);
		}
	}
}
