package com.tasteam.domain.chat.config;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.tasteam.global.security.jwt.provider.JwtTokenProvider;

@Component
public class ChatWebSocketAuthInterceptor implements ChannelInterceptor {

	private final JwtTokenProvider jwtTokenProvider;

	public ChatWebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
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
		return message;
	}

}
