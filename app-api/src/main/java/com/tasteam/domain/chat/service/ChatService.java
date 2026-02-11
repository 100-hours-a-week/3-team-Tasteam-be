package com.tasteam.domain.chat.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.chat.dto.ChatMessageCursor;
import com.tasteam.domain.chat.dto.ChatMessageQueryDto;
import com.tasteam.domain.chat.dto.request.ChatMessageSendRequest;
import com.tasteam.domain.chat.dto.request.ChatReadCursorUpdateRequest;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageListResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageSendResponse;
import com.tasteam.domain.chat.dto.response.ChatReadCursorUpdateResponse;
import com.tasteam.domain.chat.entity.ChatMessage;
import com.tasteam.domain.chat.entity.ChatRoomMember;
import com.tasteam.domain.chat.repository.ChatMessageRepository;
import com.tasteam.domain.chat.repository.ChatRoomMemberRepository;
import com.tasteam.domain.chat.repository.ChatRoomRepository;
import com.tasteam.domain.chat.type.ChatMessageType;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.ChatErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.global.utils.CursorPageBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final CursorCodec cursorCodec;

	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessages(Long chatRoomId, Long memberId, String cursor, Integer size) {
		int resolvedSize = resolvePageSize(size);
		ensureChatRoomExists(chatRoomId);
		ChatRoomMember membership = findMembershipOrThrow(chatRoomId, memberId);

		CursorPageBuilder<ChatMessageCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursor,
			ChatMessageCursor.class);
		if (pageBuilder.isInvalid()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		Long cursorId = pageBuilder.cursor() == null ? null : pageBuilder.cursor().messageId();
		List<ChatMessageQueryDto> results = chatMessageRepository.findMessagePage(
			chatRoomId,
			cursorId,
			PageRequest.of(0, CursorPageBuilder.fetchSize(resolvedSize)));

		CursorPageBuilder.Page<ChatMessageQueryDto> page = pageBuilder.build(
			results,
			resolvedSize,
			last -> new ChatMessageCursor(last.id()));

		List<ChatMessageItemResponse> items = page.items().stream()
			.map(item -> new ChatMessageItemResponse(
				item.id(),
				item.memberId(),
				item.memberNickname(),
				item.memberProfileImageUrl(),
				item.content(),
				item.messageType(),
				item.createdAt()))
			.toList();

		return new ChatMessageListResponse(
			new ChatMessageListResponse.Meta(membership.getLastReadMessageId()),
			items,
			new ChatMessageListResponse.Page(page.nextCursor(), resolvedSize, page.hasNext()));
	}

	@Transactional
	public ChatMessageSendResponse sendMessage(Long chatRoomId, Long memberId, ChatMessageSendRequest request) {
		ensureChatRoomExists(chatRoomId);
		findMembershipOrThrow(chatRoomId, memberId);

		ChatMessageType messageType = request.messageType() == null ? ChatMessageType.TEXT : request.messageType();
		validateMessageTypeAndContent(messageType, request.content());
		String content = messageType == ChatMessageType.TEXT ? request.content() : null;

		ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
			.chatRoomId(chatRoomId)
			.memberId(memberId)
			.type(messageType)
			.content(content)
			.deletedAt(null)
			.build());

		return new ChatMessageSendResponse(
			message.getId(),
			message.getType(),
			message.getContent(),
			null,
			message.getCreatedAt());
	}

	@Transactional
	public ChatReadCursorUpdateResponse updateReadCursor(Long chatRoomId, Long memberId,
		ChatReadCursorUpdateRequest request) {
		ensureChatRoomExists(chatRoomId);
		findMembershipOrThrow(chatRoomId, memberId);

		Long lastReadMessageId = request.lastReadMessageId();
		if (!chatMessageRepository.existsByIdAndChatRoomIdAndDeletedAtIsNull(lastReadMessageId, chatRoomId)) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		Instant now = Instant.now();
		int updated = chatRoomMemberRepository.updateLastReadMessageId(chatRoomId, memberId, lastReadMessageId, now);
		if (updated == 0) {
			throw new BusinessException(CommonErrorCode.NO_PERMISSION);
		}

		ChatRoomMember membership = findMembershipOrThrow(chatRoomId, memberId);

		return new ChatReadCursorUpdateResponse(
			chatRoomId,
			memberId,
			membership.getLastReadMessageId(),
			membership.getUpdatedAt());
	}

	private void ensureChatRoomExists(Long chatRoomId) {
		if (chatRoomRepository.findByIdAndDeletedAtIsNull(chatRoomId).isEmpty()) {
			throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
		}
	}

	private ChatRoomMember findMembershipOrThrow(Long chatRoomId, Long memberId) {
		return chatRoomMemberRepository.findByChatRoomIdAndMemberIdAndDeletedAtIsNull(chatRoomId, memberId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NO_PERMISSION));
	}

	private int resolvePageSize(Integer size) {
		if (size == null) {
			return DEFAULT_PAGE_SIZE;
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return size;
	}

	private void validateMessageTypeAndContent(ChatMessageType messageType, String content) {
		if (messageType == ChatMessageType.SYSTEM) {
			throw new BusinessException(CommonErrorCode.NO_PERMISSION);
		}
		if (messageType == ChatMessageType.TEXT) {
			if (content == null || content.trim().isEmpty()) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			if (content.length() > 500) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
		}
	}
}
