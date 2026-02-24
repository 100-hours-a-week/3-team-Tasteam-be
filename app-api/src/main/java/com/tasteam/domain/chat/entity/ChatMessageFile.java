package com.tasteam.domain.chat.entity;

import java.time.Instant;

import com.tasteam.domain.chat.type.ChatMessageFileType;
import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
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
@Table(name = "chat_message_file")
public class ChatMessageFile extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_message_file_seq_gen")
	@SequenceGenerator(name = "chat_message_file_seq_gen", sequenceName = "chat_message_file_id_seq", allocationSize = 1)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "chat_message_id", nullable = false)
	private Long chatMessageId;

	@Enumerated(EnumType.STRING)
	@Column(name = "file_type", nullable = false, length = 20)
	private ChatMessageFileType fileType;

	@Column(name = "file_uuid", length = 36)
	private String fileUuid;

	@Column(name = "file_url", length = 500)
	private String fileUrl;

	@Column(name = "deleted_at")
	private Instant deletedAt;
}
