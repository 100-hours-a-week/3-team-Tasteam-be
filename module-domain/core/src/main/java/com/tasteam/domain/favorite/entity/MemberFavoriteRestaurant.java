package com.tasteam.domain.favorite.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "member_favorite_restaurant", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id",
	"restaurant_id"}))
public class MemberFavoriteRestaurant extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "restaurant_id", nullable = false)
	private Long restaurantId;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static MemberFavoriteRestaurant create(Long memberId, Long restaurantId) {
		return MemberFavoriteRestaurant.builder()
			.memberId(memberId)
			.restaurantId(restaurantId)
			.build();
	}

	public void delete() {
		this.deletedAt = Instant.now();
	}

	public void restore() {
		this.deletedAt = null;
	}
}
