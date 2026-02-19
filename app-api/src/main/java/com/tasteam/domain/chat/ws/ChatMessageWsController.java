package com.tasteam.domain.chat.ws;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.tasteam.domain.chat.dto.request.ChatMessageSendRequest;
import com.tasteam.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageWsController {
	private final ChatService chatService;

	@MessageMapping("/chat-rooms/{chatRoomId}/messages")
	public void send(
		@DestinationVariable
		Long chatRoomId,
		ChatMessageSendRequest request,
		Principal principal,
		@Header(name = "simpSessionId", required = false)
		String sessionId) {
		String principalName = principal != null ? principal.getName() : null;
		log.info("WS SEND received. chatRoomId={}, principal={}, sessionId={}",
			chatRoomId, principalName, sessionId);
		if (principalName == null) {
			log.warn("WS SEND rejected. reason=missing_principal, chatRoomId={}, sessionId={}",
				chatRoomId, sessionId);
			return;
		}
		Long memberId = Long.valueOf(principalName);

		chatService.sendMessage(chatRoomId, memberId, request);
	}
}
