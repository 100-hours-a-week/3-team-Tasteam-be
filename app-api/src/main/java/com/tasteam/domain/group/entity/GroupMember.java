package com.tasteam.domain.group.entity;

import java.time.Instant;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseCreatedAtEntity;
import com.tasteam.domain.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "group_member", uniqueConstraints = {
	@UniqueConstraint(name = "uk_group_member_group_id_member_id", columnNames = {"group_id", "member_id"})
})
@Comment("특정 그룹에 가입한 회원들의 가입, 탈퇴 상태를 관리하는 매핑 테이블")
public class GroupMember extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static GroupMember create(Long groupId, Member member) {
		return GroupMember.builder()
			.groupId(groupId)
			.member(member)
			.deletedAt(null)
			.build();
	}

	public void softDelete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
