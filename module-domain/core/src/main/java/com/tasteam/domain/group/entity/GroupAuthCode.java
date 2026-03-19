package com.tasteam.domain.group.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseCreatedAtEntity;

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
@Table(name = "group_auth_code")
public class GroupAuthCode extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "code", nullable = false, length = 255)
	private String code;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "verified_at")
	private Instant verifiedAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	public void verify(Instant verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	public void expire(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public static GroupAuthCode create(Long groupId, String code, Instant expiresAt) {
		return GroupAuthCode.builder()
			.groupId(groupId)
			.code(code)
			.expiresAt(expiresAt)
			.build();
	}
}
