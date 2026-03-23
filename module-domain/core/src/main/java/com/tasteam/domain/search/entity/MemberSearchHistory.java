package com.tasteam.domain.search.entity;

import java.time.Instant;

import org.hibernate.annotations.Comment;

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

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_search_history")
@Comment("회원 검색어 기록")
public class MemberSearchHistory extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "keyword", nullable = false, length = 100)
	private String keyword;

	@Column(name = "count", nullable = false)
	private Long count;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static MemberSearchHistory create(Long memberId, String keyword) {
		return MemberSearchHistory.builder()
			.memberId(memberId)
			.keyword(keyword)
			.count(1L)
			.deletedAt(null)
			.build();
	}

	public void incrementCount() {
		this.count = this.count + 1L;
	}

	public void delete() {
		this.deletedAt = Instant.now();
	}
}
