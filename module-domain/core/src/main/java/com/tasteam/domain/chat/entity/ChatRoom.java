package com.tasteam.domain.chat.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "chat_room")
public class ChatRoom extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_room_seq_gen")
	@SequenceGenerator(name = "chat_room_seq_gen", sequenceName = "chat_room_id_seq", allocationSize = 1)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "subgroup_id", nullable = false)
	private Long subgroupId;

	@Column(name = "deleted_at")
	private Instant deletedAt;
}
