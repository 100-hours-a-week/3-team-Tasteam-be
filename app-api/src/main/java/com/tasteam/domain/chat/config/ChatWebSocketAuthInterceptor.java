package com.tasteam.domain.chat.config;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.tasteam.domain.chat.repository.ChatRoomMemberRepository;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;

@Component
public class ChatWebSocketAuthInterceptor implements ChannelInterceptor {

	private final JwtTokenProvider jwtTokenProvider;
	private final ChatRoomMemberRepository chatRoomMemberRepository;

	public ChatWebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider,
		ChatRoomMemberRepository chatRoomMemberRepository) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.chatRoomMemberRepository = chatRoomMemberRepository;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
			String token = authHeader != null && authHeader.startsWith("Bearer ")
				? authHeader.substring(7)
				: null;

			if (token == null || jwtTokenProvider.isTokenExpired(token) || !jwtTokenProvider.isAccessToken(token)) {
				throw new IllegalArgumentException("Invalid token");
			}

			Long memberId = jwtTokenProvider.getUidFromToken(token);

			Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
			if (sessionAttributes != null) {
				sessionAttributes.put("memberId", memberId);
			}

			accessor.setUser(() -> String.valueOf(memberId));

		}
		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			Long memberId = resolveMemberId(accessor);
			String destination = accessor.getDestination();
			if (memberId == null || destination == null) {
				throw new IllegalArgumentException("Invalid subscription");
			}
			Long chatRoomId = parseChatRoomId(destination);
			if (chatRoomId == null) {
				throw new IllegalArgumentException("Invalid subscription");
			}
			boolean hasMembership = chatRoomMemberRepository
				.existsByChatRoomIdAndMemberIdAndDeletedAtIsNull(chatRoomId, memberId);
			if (!hasMembership) {
				throw new IllegalArgumentException("No permission");
			}
		}
		return message;
	}

	private Long resolveMemberId(StompHeaderAccessor accessor) {
		if (accessor.getUser() != null) {
			try {
				return Long.valueOf(accessor.getUser().getName());
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
		if (sessionAttributes != null) {
			Object value = sessionAttributes.get("memberId");
			if (value instanceof Long memberId) {
				return memberId;
			}
			if (value instanceof String stringValue) {
				try {
					return Long.valueOf(stringValue);
				} catch (NumberFormatException ignored) {
					return null;
				}
			}
		}
		return null;
	}

	private Long parseChatRoomId(String destination) {
		String prefix = "/topic/chat-rooms/";
		if (!destination.startsWith(prefix)) {
			return null;
		}
		String idPart = destination.substring(prefix.length());
		try {
			return Long.valueOf(idPart);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

}
