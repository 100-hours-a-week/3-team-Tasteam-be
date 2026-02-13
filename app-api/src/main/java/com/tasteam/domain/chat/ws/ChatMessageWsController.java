package com.tasteam.domain.chat.ws;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.tasteam.domain.chat.dto.request.ChatMessageSendRequest;
import com.tasteam.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatMessageWsController {
	private final ChatService chatService;

	@MessageMapping("/chat-rooms/{chatRoomId}/messages")
	public void send(
		@DestinationVariable
		Long chatRoomId,
		ChatMessageSendRequest request,
		Principal principal) {
		Long memberId = Long.valueOf(principal.getName());

		chatService.sendMessage(chatRoomId, memberId, request);
	}
}
