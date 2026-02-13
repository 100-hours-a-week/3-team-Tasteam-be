package com.tasteam.domain.chat.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.chat.entity.ChatRoomMember;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

	Optional<ChatRoomMember> findByChatRoomIdAndMemberIdAndDeletedAtIsNull(Long chatRoomId, Long memberId);

	Optional<ChatRoomMember> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

	boolean existsByChatRoomIdAndMemberIdAndDeletedAtIsNull(Long chatRoomId, Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ChatRoomMember m
		set m.lastReadMessageId = case
			when m.lastReadMessageId is null or m.lastReadMessageId < :newId then :newId
			else m.lastReadMessageId
		end,
		m.updatedAt = :now
		where m.chatRoomId = :chatRoomId
		and m.memberId = :memberId
		and m.deletedAt is null
		""")
	int updateLastReadMessageId(
		@Param("chatRoomId")
		Long chatRoomId,
		@Param("memberId")
		Long memberId,
		@Param("newId")
		Long newId,
		@Param("now")
		Instant now);
}
