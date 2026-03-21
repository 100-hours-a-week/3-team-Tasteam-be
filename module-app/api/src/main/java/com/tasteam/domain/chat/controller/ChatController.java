package com.tasteam.domain.chat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.chat.controller.docs.ChatControllerDocs;
import com.tasteam.domain.chat.dto.request.ChatMessageSendRequest;
import com.tasteam.domain.chat.dto.request.ChatReadCursorUpdateRequest;
import com.tasteam.domain.chat.dto.response.ChatMessageListResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageSendResponse;
import com.tasteam.domain.chat.dto.response.ChatReadCursorUpdateResponse;
import com.tasteam.domain.chat.service.ChatService;
import com.tasteam.domain.chat.type.ChatMessageListMode;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
@Validated
public class ChatController implements ChatControllerDocs {

	private final ChatService chatService;

	@GetMapping("/{chatRoomId}/messages")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<ChatMessageListResponse> getMessages(
		@PathVariable @Positive
		Long chatRoomId,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		ChatMessageListMode mode,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100)
		int size,
		@CurrentUser
		Long memberId) {
		return SuccessResponse.success(chatService.getMessages(chatRoomId, memberId, cursor, mode, size));
	}

	@PostMapping("/{chatRoomId}/messages")
	@PreAuthorize("hasRole('USER')")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<ChatMessageSendResponse> sendMessage(
		@PathVariable @Positive
		Long chatRoomId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		ChatMessageSendRequest request) {
		return SuccessResponse.success(chatService.sendMessage(chatRoomId, memberId, request));
	}

	@PatchMapping("/{chatRoomId}/read-cursor")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<ChatReadCursorUpdateResponse> updateReadCursor(
		@PathVariable @Positive
		Long chatRoomId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		ChatReadCursorUpdateRequest request) {
		return SuccessResponse.success(chatService.updateReadCursor(chatRoomId, memberId, request));
	}
}
