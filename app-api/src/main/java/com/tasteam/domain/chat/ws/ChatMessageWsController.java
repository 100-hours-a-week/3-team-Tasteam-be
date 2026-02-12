package com.tasteam.domain.chat.ws;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.tasteam.domain.chat.dto.request.ChatMessageSendRequest;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatMessageWsController {
	private final ChatService chatService;
	private final SimpMessagingTemplate simpMessagingTemplate;

	@MessageMapping("/chat-rooms/{chatRoomId}/messages")
	public void send(
		@DestinationVariable
		Long chatRoomId,
		ChatMessageSendRequest request,
		Principal principal) {
		Long memberId = Long.valueOf(principal.getName());

		ChatMessageItemResponse message = chatService.sendMessage(chatRoomId, memberId, request).data();

		simpMessagingTemplate.convertAndSend("/topic/chat-rooms/" + chatRoomId, message);
	}
}
