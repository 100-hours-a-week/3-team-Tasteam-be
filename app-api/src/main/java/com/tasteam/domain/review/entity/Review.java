package com.tasteam.domain.review.entity;

import java.time.Instant;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseTimeEntity;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.restaurant.entity.Restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review")
@Comment("그룹/하위 그룹 단위로 작성되는 음식점 리뷰")
public class Review extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "subgroup_id")
	private Long subgroupId;

	@Column(name = "content", length = 1000)
	private String content;

	@Column(name = "is_recommended", nullable = false)
	private boolean isRecommended;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "vector_synced_at")
	@Comment("해당 리뷰 벡터 업로드 완료 시각")
	private Instant vectorSyncedAt;

	public static Review create(
		Restaurant restaurant,
		Member member,
		Long groupId,
		Long subgroupId,
		String content,
		boolean isRecommended) {
		return Review.builder()
			.restaurant(restaurant)
			.member(member)
			.groupId(groupId)
			.subgroupId(subgroupId)
			.content(content)
			.isRecommended(isRecommended)
			.deletedAt(null)
			.vectorSyncedAt(null)
			.build();
	}

	public void softDelete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	/**
	 * 벡터 업로드 성공 시 호출. 해당 리뷰의 vector_synced_at을 갱신한다.
	 */
	public void markVectorSynced(Instant syncedAt) {
		this.vectorSyncedAt = syncedAt;
	}
}
