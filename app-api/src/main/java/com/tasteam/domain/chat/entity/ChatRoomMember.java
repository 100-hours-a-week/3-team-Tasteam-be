package com.tasteam.domain.chat.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "chat_room_member")
public class ChatRoomMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "chat_room_id", nullable = false)
	private Long chatRoomId;

	@Column(name = "last_read_message_id")
	private Long lastReadMessageId;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public void updateLastReadMessageId(Long newId) {
		if (newId == null) {
			return;
		}
		if (lastReadMessageId == null || newId > lastReadMessageId) {
			lastReadMessageId = newId;
		}
	}
}
