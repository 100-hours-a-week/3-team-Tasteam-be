package com.tasteam.domain.subgroup.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseTimeEntity;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.domain.subgroup.type.SubgroupStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "\"subgroup\"")
public class Subgroup extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_id", nullable = false)
	private Group group;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "join_type", nullable = false, length = 20)
	private SubgroupJoinType joinType;

	@Column(name = "join_password", length = 255)
	private String joinPassword;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SubgroupStatus status;

	@Column(name = "member_count", nullable = false)
	private Integer memberCount;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public void updateName(String name) {
		this.name = name;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public void updateProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public void increaseMemberCount() {
		if (this.memberCount == null) {
			this.memberCount = 0;
		}
		this.memberCount = this.memberCount + 1;
	}

	public void decreaseMemberCount() {
		if (this.memberCount == null || this.memberCount <= 0) {
			this.memberCount = 0;
			return;
		}
		this.memberCount = this.memberCount - 1;
	}

	public void delete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
