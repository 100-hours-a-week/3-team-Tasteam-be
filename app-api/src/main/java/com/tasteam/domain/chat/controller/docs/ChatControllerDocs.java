package com.tasteam.domain.chat.controller.docs;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.chat.dto.request.ChatMessageSendRequest;
import com.tasteam.domain.chat.dto.request.ChatReadCursorUpdateRequest;
import com.tasteam.domain.chat.dto.response.ChatMessageListResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageSendResponse;
import com.tasteam.domain.chat.dto.response.ChatReadCursorUpdateResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@SwaggerTagOrder(55)
@Tag(name = "Chat", description = "채팅 API")
public interface ChatControllerDocs {

	@Operation(summary = "채팅 메시지 목록 조회", description = "채팅방 메시지를 커서 기반으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ChatMessageListResponse.class)))
	SuccessResponse<ChatMessageListResponse> getMessages(
		@Parameter(description = "채팅방 ID", example = "10") @PathVariable @Positive
		Long chatRoomId,
		@Parameter(description = "커서", example = "opaque") @RequestParam(required = false)
		String cursor,
		@Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100)
		int size,
		@CurrentUser
		Long memberId);

	@Operation(summary = "채팅 메시지 전송", description = "채팅방에 메시지를 전송합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = ChatMessageSendRequest.class)))
	@ApiResponse(responseCode = "201", description = "전송 성공", content = @Content(schema = @Schema(implementation = ChatMessageSendResponse.class)))
	SuccessResponse<ChatMessageSendResponse> sendMessage(
		@Parameter(description = "채팅방 ID", example = "10") @PathVariable @Positive
		Long chatRoomId,
		@CurrentUser
		Long memberId,
		@Validated
		ChatMessageSendRequest request);

	@Operation(summary = "읽음 커서 갱신", description = "마지막으로 읽은 메시지 ID를 갱신합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = ChatReadCursorUpdateRequest.class)))
	@ApiResponse(responseCode = "200", description = "갱신 성공", content = @Content(schema = @Schema(implementation = ChatReadCursorUpdateResponse.class)))
	SuccessResponse<ChatReadCursorUpdateResponse> updateReadCursor(
		@Parameter(description = "채팅방 ID", example = "10") @PathVariable @Positive
		Long chatRoomId,
		@CurrentUser
		Long memberId,
		@Validated
		ChatReadCursorUpdateRequest request);
}
