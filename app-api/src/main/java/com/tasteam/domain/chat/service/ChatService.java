package com.tasteam.domain.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
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
import com.tasteam.domain.chat.event.ChatMessageSentEvent;
import com.tasteam.domain.chat.repository.ChatMessageFileRepository;
import com.tasteam.domain.chat.repository.ChatMessageRepository;
import com.tasteam.domain.chat.repository.ChatRoomMemberRepository;
import com.tasteam.domain.chat.repository.ChatRoomRepository;
import com.tasteam.domain.chat.stream.ChatStreamPublisher;
import com.tasteam.domain.chat.type.ChatMessageFileType;
import com.tasteam.domain.chat.type.ChatMessageListMode;
import com.tasteam.domain.chat.type.ChatMessageType;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.ChatErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FileErrorCode;
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
	private final ImageRepository imageRepository;
	private final DomainImageRepository domainImageRepository;
	private final FileService fileService;
	private final ChatStreamPublisher chatStreamPublisher;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessages(Long chatRoomId, Long memberId, String cursor, ChatMessageListMode mode,
		Integer size) {
		int resolvedSize = resolvePageSize(size);
		ensureChatRoomExists(chatRoomId);
		ChatRoomMember membership = findMembershipOrThrow(chatRoomId, memberId);

		ChatMessageListMode resolvedMode = mode == null ? ChatMessageListMode.BEFORE : mode;
		String rawCursor = resolvedMode == ChatMessageListMode.ENTER ? null : cursor;
		CursorPageBuilder<ChatMessageCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, rawCursor,
			ChatMessageCursor.class);
		if (pageBuilder.isInvalid()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		CursorPageBuilder.Page<ChatMessageQueryDto> page;
		List<ChatMessageQueryDto> pageItems;
		// 기준 포함 여부: ENTER는 기준(lastReadMessageId)을 포함하고, BEFORE/AFTER는 커서 기준을 포함하지 않습니다.
		switch (resolvedMode) {
			case ENTER -> {
				Long baseId = membership.getLastReadMessageId();
				List<ChatMessageQueryDto> results = chatMessageRepository.findMessagePageUpTo(
					chatRoomId,
					baseId,
					PageRequest.of(0, CursorPageBuilder.fetchSize(resolvedSize)));
				page = pageBuilder.build(results, resolvedSize, last -> new ChatMessageCursor(last.id()));
				pageItems = page.items();
			}
			case AFTER -> {
				if (pageBuilder.cursor() == null) {
					throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
				}
				Long cursorId = pageBuilder.cursor().messageId();
				List<ChatMessageQueryDto> results = chatMessageRepository.findMessagePageAfter(
					chatRoomId,
					cursorId,
					PageRequest.of(0, CursorPageBuilder.fetchSize(resolvedSize)));
				page = pageBuilder.build(results, resolvedSize, last -> new ChatMessageCursor(last.id()));
				pageItems = new ArrayList<>(page.items());
				java.util.Collections.reverse(pageItems);
			}
			case BEFORE -> {
				Long cursorId = pageBuilder.cursor() == null ? null : pageBuilder.cursor().messageId();
				List<ChatMessageQueryDto> results = chatMessageRepository.findMessagePage(
					chatRoomId,
					cursorId,
					PageRequest.of(0, CursorPageBuilder.fetchSize(resolvedSize)));
				page = pageBuilder.build(results, resolvedSize, last -> new ChatMessageCursor(last.id()));
				pageItems = page.items();
			}
			default -> throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		List<Long> memberIds = pageItems.stream()
			.map(ChatMessageQueryDto::memberId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		Map<Long, String> imageUrlByMemberId = fileService.getPrimaryDomainImageUrlMap(
			DomainType.MEMBER,
			memberIds);

		Map<Long, List<ChatMessageFileItemResponse>> filesByMessageId = loadMessageFiles(
			pageItems.stream().map(ChatMessageQueryDto::id).toList());

		List<ChatMessageItemResponse> items = pageItems.stream()
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

		String afterCursor = pageItems.isEmpty()
			? null
			: cursorCodec.encode(new ChatMessageCursor(pageItems.get(0).id()));

		return new ChatMessageListResponse(
			new ChatMessageListResponse.Meta(membership.getLastReadMessageId()),
			items,
			new ChatMessageListResponse.Page(page.nextCursor(), afterCursor, resolvedSize, page.hasNext()));
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
		eventPublisher.publishEvent(new ChatMessageSentEvent(
			chatRoomId,
			message.getId(),
			memberId,
			member.getNickname(),
			message.getType(),
			message.getContent(),
			message.getCreatedAt()));

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
			Image image = activateAndGetImage(fileUuid);
			DomainImage domainImage = domainImageRepository.save(
				DomainImage.create(DomainType.CHAT_MESSAGE, chatMessageId, image, 0));
			ChatMessageFile messageFile = chatMessageFileRepository.save(ChatMessageFile.builder()
				.chatMessageId(chatMessageId)
				.fileType(ChatMessageFileType.IMAGE)
				.domainImageId(domainImage.getId())
				.fileUuid(fileUuid)
				.fileUrl(null)
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
		Map<Long, String> domainImageUrlById = resolveDomainImageUrlMap(
			files.stream().map(ChatMessageFile::getDomainImageId).toList());
		return files.stream()
			.map(file -> new ChatMessageFileItemResponse(
				file.getFileType(),
				resolveChatFileUrl(file, domainImageUrlById)))
			.toList();
	}

	private Map<Long, List<ChatMessageFileItemResponse>> loadMessageFiles(List<Long> messageIds) {
		if (messageIds == null || messageIds.isEmpty()) {
			return Map.of();
		}
		List<ChatMessageFile> files = chatMessageFileRepository.findAllByChatMessageIdInAndDeletedAtIsNull(messageIds);
		Map<Long, String> domainImageUrlById = resolveDomainImageUrlMap(
			files.stream().map(ChatMessageFile::getDomainImageId).toList());
		return files.stream()
			.collect(java.util.stream.Collectors.groupingBy(
				ChatMessageFile::getChatMessageId,
				java.util.stream.Collectors.mapping(
					file -> new ChatMessageFileItemResponse(file.getFileType(),
						resolveChatFileUrl(file, domainImageUrlById)),
					java.util.stream.Collectors.toList())));
	}

	private String resolveChatFileUrl(ChatMessageFile file, Map<Long, String> domainImageUrlById) {
		Long domainImageId = file.getDomainImageId();
		if (domainImageId != null) {
			String url = domainImageUrlById.get(domainImageId);
			if (url != null) {
				return url;
			}
		}
		if (file.getFileUuid() != null && !file.getFileUuid().isBlank()) {
			return fileService.getImageUrl(file.getFileUuid()).url();
		}
		return file.getFileUrl();
	}

	private Map<Long, String> resolveDomainImageUrlMap(List<Long> domainImageIds) {
		if (domainImageIds == null || domainImageIds.isEmpty()) {
			return Map.of();
		}
		List<Long> filtered = domainImageIds.stream()
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		if (filtered.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> result = new java.util.HashMap<>();
		for (DomainImage domainImage : domainImageRepository.findAllById(filtered)) {
			Image image = domainImage.getImage();
			if (image == null || image.getFileUuid() == null) {
				continue;
			}
			result.put(domainImage.getId(), fileService.getPublicUrl(image.getStorageKey()));
		}
		return result;
	}

	private Image activateAndGetImage(String fileUuid) {
		java.util.UUID uuid;
		try {
			uuid = java.util.UUID.fromString(fileUuid);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "fileUuid 형식이 올바르지 않습니다");
		}

		Image image = imageRepository.findByFileUuid(uuid)
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		if (image.getStatus() == ImageStatus.DELETED) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}
		if (image.getStatus() == ImageStatus.PENDING) {
			image.activate();
		}
		return image;
	}
}
