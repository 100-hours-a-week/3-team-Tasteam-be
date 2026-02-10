package com.tasteam.domain.favorite.entity;

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

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "subgroup_favorite_restaurant")
public class SubgroupFavoriteRestaurant extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "subgroup_id", nullable = false)
	private Long subgroupId;

	@Column(name = "restaurant_id", nullable = false)
	private Long restaurantId;

	@Column(name = "deleted_at")
	private java.time.Instant deletedAt;

	public static SubgroupFavoriteRestaurant create(Long memberId, Long subgroupId, Long restaurantId) {
		return SubgroupFavoriteRestaurant.builder()
			.memberId(memberId)
			.subgroupId(subgroupId)
			.restaurantId(restaurantId)
			.build();
	}

	public void delete() {
		this.deletedAt = java.time.Instant.now();
	}

	public void restore() {
		this.deletedAt = null;
	}
}
