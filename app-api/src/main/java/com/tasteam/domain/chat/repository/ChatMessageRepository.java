package com.tasteam.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.chat.dto.ChatMessageQueryDto;
import com.tasteam.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	@Query("""
		select new com.tasteam.domain.chat.dto.ChatMessageQueryDto(
			m.id,
			m.memberId,
			member.nickname,
			m.content,
			m.type,
			m.createdAt
		)
		from ChatMessage m
		left join Member member on member.id = m.memberId
		where m.chatRoomId = :chatRoomId
		and m.deletedAt is null
		and (:cursorId is null or m.id < :cursorId)
		order by m.id desc
		""")
	List<ChatMessageQueryDto> findMessagePage(
		@Param("chatRoomId")
		Long chatRoomId,
		@Param("cursorId")
		Long cursorId,
		Pageable pageable);

	boolean existsByIdAndChatRoomIdAndDeletedAtIsNull(Long id, Long chatRoomId);
}
