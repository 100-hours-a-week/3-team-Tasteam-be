package com.tasteam.domain.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.chat.dto.request.ChatMessageSendRequest;
import com.tasteam.domain.chat.dto.request.ChatReadCursorUpdateRequest;
import com.tasteam.domain.chat.dto.response.ChatMessageFileItemResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageListResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageSendResponse;
import com.tasteam.domain.chat.dto.response.ChatReadCursorUpdateResponse;
import com.tasteam.domain.chat.service.ChatService;
import com.tasteam.domain.chat.type.ChatMessageFileType;
import com.tasteam.domain.chat.type.ChatMessageListMode;
import com.tasteam.domain.chat.type.ChatMessageType;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.ChatErrorCode;

@ControllerWebMvcTest(ChatController.class)
@DisplayName("[유닛](Chat) ChatController 단위 테스트")
class ChatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ChatService chatService;

	@Nested
	@DisplayName("채팅 메시지 목록 조회")
	class GetMessages {

		@Test
		@DisplayName("채팅방 메시지를 조회하면 메시지 목록을 반환한다")
		void 채팅_메시지_목록_조회_성공() throws Exception {
			// given
			var item = new ChatMessageItemResponse(
				10L,
				20L,
				"보내는이",
				"https://example.com/profile.jpg",
				"안녕하세요",
				ChatMessageType.TEXT,
				List.of(new ChatMessageFileItemResponse(ChatMessageFileType.IMAGE, "https://example.com/image.png")),
				Instant.parse("2026-02-01T10:00:00Z"));
			var response = new ChatMessageListResponse(
				new ChatMessageListResponse.Meta(5L),
				List.of(item),
				new ChatMessageListResponse.Page("next-cursor", "after-cursor", 20, false));
			given(chatService.getMessages(anyLong(), anyLong(), any(), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/messages", 1L)
				.param("size", "20")
				.param("cursor", "cursor-1")
				.param("mode", ChatMessageListMode.BEFORE.name()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.meta.lastReadMessageId").value(5))
				.andExpect(jsonPath("$.data.data[0].id").value(10))
				.andExpect(jsonPath("$.data.data[0].messageType").value("TEXT"))
				.andExpect(jsonPath("$.data.page.nextCursor").value("next-cursor"));
		}

		@Test
		@DisplayName("사이즈가 범위를 벗어나면 내부 처리 오류로 실패한다")
		void 채팅_메시지_조회_사이즈_범위_오류_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/messages", 1L)
				.param("size", "0"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}

		@Test
		@DisplayName("채팅방이 존재하지 않으면 404로 실패한다")
		void 채팅_메시지_조회_채팅방_없음_실패() throws Exception {
			// given
			given(chatService.getMessages(anyLong(), anyLong(), any(), any(), any()))
				.willThrow(new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/messages", 1L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("채팅 메시지 전송")
	class SendMessage {

		@Test
		@DisplayName("메시지를 전송하면 메시지 정보를 반환한다")
		void 채팅_메시지_전송_성공() throws Exception {
			// given
			var request = new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요", List.of());
			var response = new ChatMessageSendResponse(
				new ChatMessageItemResponse(
					100L,
					2L,
					"보낸이",
					"https://example.com/avatar.jpg",
					"안녕하세요",
					ChatMessageType.TEXT,
					List.of(),
					Instant.parse("2026-02-01T11:00:00Z")));
			given(chatService.sendMessage(anyLong(), anyLong(), any(ChatMessageSendRequest.class)))
				.willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/messages", 1L)
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.data.id").value(100))
				.andExpect(jsonPath("$.data.data.content").value("안녕하세요"));
		}

		@Test
		@DisplayName("채팅방 ID가 음수면 내부 처리 오류로 실패한다")
		void 채팅_메시지_전송_방식_아이디_오류_실패() throws Exception {
			// given
			var request = new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요", List.of());

			// when & then
			mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/messages", -1L)
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}

		@Test
		@DisplayName("채팅방이 존재하지 않으면 404로 실패한다")
		void 채팅_메시지_전송_채팅방_없음_실패() throws Exception {
			// given
			var request = new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요", List.of());
			given(chatService.sendMessage(anyLong(), anyLong(), any(ChatMessageSendRequest.class)))
				.willThrow(new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

			// when & then
			mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/messages", 1L)
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("채팅 읽음 포인터 업데이트")
	class UpdateReadCursor {

		@Test
		@DisplayName("읽음 포인터를 갱신하면 최신 포인트 정보를 반환한다")
		void 채팅_읽음포인터_업데이트_성공() throws Exception {
			// given
			var request = new ChatReadCursorUpdateRequest(30L);
			var response = new ChatReadCursorUpdateResponse(1L, 1L, 30L, Instant.parse("2026-02-01T11:30:00Z"));
			given(chatService.updateReadCursor(anyLong(), anyLong(), any(ChatReadCursorUpdateRequest.class)))
				.willReturn(response);

			// when & then
			mockMvc.perform(patch("/api/v1/chat-rooms/{chatRoomId}/read-cursor", 1L)
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.lastReadMessageId").value(30))
				.andExpect(jsonPath("$.data.roomId").value(1));
		}

		@Test
		@DisplayName("읽음 메시지 ID가 올바르지 않으면 400으로 실패한다")
		void 채팅_읽음포인터_메시지_ID_오류_실패() throws Exception {
			// given
			var request = new ChatReadCursorUpdateRequest(0L);

			// when & then
			mockMvc.perform(patch("/api/v1/chat-rooms/{chatRoomId}/read-cursor", 1L)
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}
}
