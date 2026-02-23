package com.tasteam.domain.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.chat.dto.ChatMessageCursor;
import com.tasteam.domain.chat.dto.ChatMessageQueryDto;
import com.tasteam.domain.chat.dto.request.ChatMessageFileRequest;
import com.tasteam.domain.chat.dto.request.ChatMessageSendRequest;
import com.tasteam.domain.chat.dto.request.ChatReadCursorUpdateRequest;
import com.tasteam.domain.chat.dto.response.ChatMessageFileItemResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageListResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageSendResponse;
import com.tasteam.domain.chat.dto.response.ChatReadCursorUpdateResponse;
import com.tasteam.domain.chat.entity.ChatMessage;
import com.tasteam.domain.chat.entity.ChatMessageFile;
import com.tasteam.domain.chat.entity.ChatRoomMember;
import com.tasteam.domain.chat.repository.ChatMessageFileRepository;
import com.tasteam.domain.chat.repository.ChatMessageRepository;
import com.tasteam.domain.chat.repository.ChatRoomMemberRepository;
import com.tasteam.domain.chat.repository.ChatRoomRepository;
import com.tasteam.domain.chat.stream.ChatStreamPublisher;
import com.tasteam.domain.chat.type.ChatMessageFileType;
import com.tasteam.domain.chat.type.ChatMessageType;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
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
	private final ChatMessageFileRepository chatMessageFileRepository;
	private final CursorCodec cursorCodec;
	private final MemberRepository memberRepository;
	private final FileService fileService;
	private final ChatStreamPublisher chatStreamPublisher;

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

		List<Long> memberIds = page.items().stream()
			.map(ChatMessageQueryDto::memberId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		Map<Long, String> imageUrlByMemberId = fileService.getPrimaryDomainImageUrlMap(DomainType.MEMBER, memberIds);

		Map<Long, List<ChatMessageFileItemResponse>> filesByMessageId = loadMessageFiles(
			page.items().stream().map(ChatMessageQueryDto::id).toList());

		List<ChatMessageItemResponse> items = page.items().stream()
			.map(item -> new ChatMessageItemResponse(
				item.id(),
				item.memberId(),
				item.memberNickname(),
				imageUrlByMemberId.get(item.memberId()),
				item.content(),
				item.messageType(),
				filesByMessageId.getOrDefault(item.id(), List.of()),
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
		validateMessageTypeAndContent(messageType, request.content(), request.files());
		String content = messageType == ChatMessageType.TEXT ? request.content() : null;

		ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
			.chatRoomId(chatRoomId)
			.memberId(memberId)
			.type(messageType)
			.content(content)
			.deletedAt(null)
			.build());

		List<ChatMessageFile> savedFiles = persistMessageFiles(message.getId(), request.files());
		List<ChatMessageFileItemResponse> fileItems = buildFileResponses(savedFiles);

		Member member = memberRepository.findById(memberId)
			.orElseThrow();

		ChatMessageItemResponse item = new ChatMessageItemResponse(
			message.getId(),
			message.getMemberId(),
			member.getNickname(),
			fileService.getPrimaryDomainImageUrl(DomainType.MEMBER, memberId),
			message.getContent(),
			message.getType(),
			fileItems,
			message.getCreatedAt());

		chatStreamPublisher.publish(chatRoomId, item);

		return new ChatMessageSendResponse(item);
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

	private void validateMessageTypeAndContent(ChatMessageType messageType, String content,
		List<ChatMessageFileRequest> files) {
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
			if (files != null && !files.isEmpty()) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			return;
		}
		if (messageType == ChatMessageType.FILE) {
			if (files == null || files.isEmpty() || files.size() != 1) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			ChatMessageFileRequest file = files.get(0);
			if (file == null || file.fileUuid() == null || file.fileUuid().isBlank()) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			if (content != null && !content.isBlank()) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
		}
	}

	private List<ChatMessageFile> persistMessageFiles(Long chatMessageId, List<ChatMessageFileRequest> files) {
		if (files == null || files.isEmpty()) {
			return List.of();
		}
		List<ChatMessageFile> saved = new ArrayList<>();
		for (ChatMessageFileRequest file : files) {
			String fileUuid = file.fileUuid();
			fileService.activateImage(fileUuid);
			String fileUrl = fileService.getImageUrl(fileUuid).url();
			ChatMessageFile messageFile = chatMessageFileRepository.save(ChatMessageFile.builder()
				.chatMessageId(chatMessageId)
				.fileType(ChatMessageFileType.IMAGE)
				.fileUrl(fileUrl)
				.deletedAt(null)
				.build());
			saved.add(messageFile);
		}
		return saved;
	}

	private List<ChatMessageFileItemResponse> buildFileResponses(List<ChatMessageFile> files) {
		if (files == null || files.isEmpty()) {
			return List.of();
		}
		return files.stream()
			.map(file -> new ChatMessageFileItemResponse(file.getFileType(), file.getFileUrl()))
			.toList();
	}

	private Map<Long, List<ChatMessageFileItemResponse>> loadMessageFiles(List<Long> messageIds) {
		if (messageIds == null || messageIds.isEmpty()) {
			return Map.of();
		}
		List<ChatMessageFile> files = chatMessageFileRepository.findAllByChatMessageIdInAndDeletedAtIsNull(messageIds);
		return files.stream()
			.collect(java.util.stream.Collectors.groupingBy(
				ChatMessageFile::getChatMessageId,
				java.util.stream.Collectors.mapping(
					file -> new ChatMessageFileItemResponse(file.getFileType(), file.getFileUrl()),
					java.util.stream.Collectors.toList())));
	}
}
