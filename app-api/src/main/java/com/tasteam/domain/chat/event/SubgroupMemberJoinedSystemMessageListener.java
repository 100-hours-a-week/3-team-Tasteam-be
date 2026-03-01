package com.tasteam.domain.chat.event;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.entity.ChatMessage;
import com.tasteam.domain.chat.repository.ChatMessageRepository;
import com.tasteam.domain.chat.repository.ChatRoomRepository;
import com.tasteam.domain.chat.stream.ChatStreamPublisher;
import com.tasteam.domain.chat.type.ChatMessageType;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.event.SubgroupMemberJoinedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SubgroupMemberJoinedSystemMessageListener {

	private static final String DEFAULT_SYSTEM_MESSAGE = "새 멤버가 입장했습니다.";

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatStreamPublisher chatStreamPublisher;
	private final MemberRepository memberRepository;

	@EventListener
	@Transactional
	public void onSubgroupMemberJoined(SubgroupMemberJoinedEvent event) {
		Long chatRoomId = chatRoomRepository.findBySubgroupIdAndDeletedAtIsNull(event.subgroupId())
			.map(chatRoom -> chatRoom.getId())
			.orElse(null);
		if (chatRoomId == null) {
			return;
		}

		String nickname = memberRepository.findByIdAndDeletedAtIsNull(event.memberId())
			.map(Member::getNickname)
			.orElse(null);
		String content = buildMessage(nickname);

		ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
			.chatRoomId(chatRoomId)
			.memberId(null)
			.type(ChatMessageType.SYSTEM)
			.content(content)
			.deletedAt(null)
			.build());

		ChatMessageItemResponse item = new ChatMessageItemResponse(
			message.getId(),
			null,
			"",
			"",
			message.getContent(),
			message.getType(),
			List.of(),
			message.getCreatedAt());

		chatStreamPublisher.publish(chatRoomId, item);
	}

	private String buildMessage(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			return DEFAULT_SYSTEM_MESSAGE;
		}
		return nickname + "님이 입장했습니다.";
	}
}
