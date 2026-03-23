package com.tasteam.domain.subgroup.entity;

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
import jakarta.persistence.SequenceGenerator;
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
@Table(name = "subgroup_member", uniqueConstraints = {
	@UniqueConstraint(name = "uk_subgroup_member_subgroup_id_member_id", columnNames = {"subgroup_id", "member_id"})
})
@Comment("하위그룹에 가입한 회원들의 가입, 탈퇴 상태를 관리하는 매핑 테이블")
public class SubgroupMember extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subgroup_member_seq_gen")
	@SequenceGenerator(name = "subgroup_member_seq_gen", sequenceName = "subgroup_member_seq", allocationSize = 50)
	@Column(name = "id")
	private Long id;

	@Column(name = "subgroup_id", nullable = false)
	private Long subgroupId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static SubgroupMember create(Long subgroupId, Member member) {
		return SubgroupMember.builder()
			.subgroupId(subgroupId)
			.member(member)
			.deletedAt(null)
			.build();
	}

	public void softDelete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	public void restore() {
		this.deletedAt = null;
	}
}
