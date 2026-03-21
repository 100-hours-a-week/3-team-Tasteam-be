package com.tasteam.domain.chat.config;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.tasteam.domain.chat.repository.ChatRoomMemberRepository;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

			log.info("WS CONNECT received. hasAuthHeader={}, sessionId={}",
				authHeader != null, accessor.getSessionId());

			if (token == null || jwtTokenProvider.isTokenExpired(token) || !jwtTokenProvider.isAccessToken(token)) {
				log.warn("WS CONNECT rejected. reason=invalid_token, sessionId={}", accessor.getSessionId());
				throw new IllegalArgumentException("Invalid token");
			}

			Long memberId = jwtTokenProvider.getUidFromToken(token);

			Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
			if (sessionAttributes != null) {
				sessionAttributes.put("memberId", memberId);
			}

			accessor.setUser(() -> String.valueOf(memberId));

			log.info("WS CONNECT authenticated. memberId={}, sessionId={}, headerUser={}",
				memberId,
				accessor.getSessionId(),
				accessor.getUser() != null ? accessor.getUser().getName() : null);

		}
		if (StompCommand.SEND.equals(accessor.getCommand())) {
			log.info("WS SEND pre-hydrate. sessionId={}, headerUser={}, sessionAttrsKeys={}",
				accessor.getSessionId(),
				accessor.getUser() != null ? accessor.getUser().getName() : null,
				accessor.getSessionAttributes() != null ? accessor.getSessionAttributes().keySet() : null);
			ensureUserFromSession(accessor);
			log.info("WS SEND post-hydrate. sessionId={}, headerUser={}",
				accessor.getSessionId(),
				accessor.getUser() != null ? accessor.getUser().getName() : null);
		}
		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			Long memberId = resolveMemberId(accessor);
			String destination = accessor.getDestination();
			log.info("WS SUBSCRIBE received. memberId={}, destination={}, sessionId={}",
				memberId, destination, accessor.getSessionId());
			if (memberId == null || destination == null) {
				log.warn("WS SUBSCRIBE rejected. reason=missing_member_or_destination, sessionId={}",
					accessor.getSessionId());
				throw new IllegalArgumentException("Invalid subscription");
			}
			Long chatRoomId = parseChatRoomId(destination);
			if (chatRoomId == null) {
				log.warn("WS SUBSCRIBE rejected. reason=invalid_destination, destination={}, sessionId={}",
					destination, accessor.getSessionId());
				throw new IllegalArgumentException("Invalid subscription");
			}
			boolean hasMembership = chatRoomMemberRepository
				.existsByChatRoomIdAndMemberIdAndDeletedAtIsNull(chatRoomId, memberId);
			if (!hasMembership) {
				log.warn("WS SUBSCRIBE rejected. reason=no_permission, memberId={}, chatRoomId={}, sessionId={}",
					memberId, chatRoomId, accessor.getSessionId());
				throw new IllegalArgumentException("No permission");
			}
			log.info("WS SUBSCRIBE authorized. memberId={}, chatRoomId={}, sessionId={}",
				memberId, chatRoomId, accessor.getSessionId());
		}
		return MessageBuilder.createMessage(
			message.getPayload(),
			accessor.getMessageHeaders());
	}

	private void ensureUserFromSession(StompHeaderAccessor accessor) {
		if (accessor.getUser() != null) {
			return;
		}
		Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
		if (sessionAttributes == null) {
			log.warn("WS SEND missing session attributes. sessionId={}", accessor.getSessionId());
			return;
		}
		Object memberId = sessionAttributes.get("memberId");
		if (memberId == null) {
			log.warn("WS SEND missing memberId in session. sessionId={}", accessor.getSessionId());
			return;
		}
		accessor.setUser(() -> String.valueOf(memberId));
		log.info("WS SEND user restored from session. memberId={}, sessionId={}",
			memberId, accessor.getSessionId());
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
