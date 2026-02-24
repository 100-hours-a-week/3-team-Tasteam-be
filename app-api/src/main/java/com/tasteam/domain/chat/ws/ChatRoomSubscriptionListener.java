package com.tasteam.domain.chat.ws;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.tasteam.domain.chat.stream.ChatStreamSubscriber;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRoomSubscriptionListener {

	private static final String DESTINATION_PREFIX = "/topic/chat-rooms/";

	private final ChatStreamSubscriber chatStreamSubscriber;

	@EventListener
	public void handleSubscribe(SessionSubscribeEvent event) {
		String destination = SimpMessageHeaderAccessor.wrap(event.getMessage()).getDestination();
		if (destination == null || !destination.startsWith(DESTINATION_PREFIX)) {
			return;
		}

		String roomIdText = destination.substring(DESTINATION_PREFIX.length());
		if (roomIdText.isBlank()) {
			return;
		}

		try {
			Long roomId = Long.valueOf(roomIdText);
			chatStreamSubscriber.ensureSubscribed(roomId);
		} catch (NumberFormatException ignored) {
			// Ignore invalid destination format.
		}
	}
}
