package com.tasteam.domain.chat.ws;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.tasteam.domain.chat.presence.ChatRoomPresenceRegistry;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRoomPresenceListener {

	private static final String DESTINATION_PREFIX = "/topic/chat-rooms/";

	private final ChatRoomPresenceRegistry presenceRegistry;

	@EventListener
	public void onSubscribe(SessionSubscribeEvent event) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
		String destination = accessor.getDestination();
		if (destination == null || !destination.startsWith(DESTINATION_PREFIX)) {
			return;
		}
		Long chatRoomId = parseRoomId(destination);
		if (chatRoomId == null) {
			return;
		}

		String sessionId = accessor.getSessionId();
		String subscriptionId = accessor.getSubscriptionId();
		Long memberId = resolveMemberId(accessor.getUser());

		presenceRegistry.registerSubscription(sessionId, subscriptionId, memberId, chatRoomId);
	}

	@EventListener
	public void onUnsubscribe(SessionUnsubscribeEvent event) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		String subscriptionId = accessor.getSubscriptionId();
		Long memberId = resolveMemberId(accessor.getUser());
		presenceRegistry.unregisterSubscription(sessionId, subscriptionId, memberId);
	}

	@EventListener
	public void onDisconnect(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		Long memberId = resolveMemberId(accessor.getUser());
		presenceRegistry.unregisterAll(sessionId, memberId);
	}

	private Long parseRoomId(String destination) {
		String roomIdText = destination.substring(DESTINATION_PREFIX.length());
		if (roomIdText.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(roomIdText);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private Long resolveMemberId(Principal principal) {
		if (principal == null || principal.getName() == null) {
			return null;
		}
		try {
			return Long.valueOf(principal.getName());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
}
