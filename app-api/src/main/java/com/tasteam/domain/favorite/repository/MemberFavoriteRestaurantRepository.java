package com.tasteam.domain.favorite.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.favorite.entity.MemberFavoriteRestaurant;

public interface MemberFavoriteRestaurantRepository extends JpaRepository<MemberFavoriteRestaurant, Long> {

	Optional<MemberFavoriteRestaurant> findByMemberIdAndRestaurantId(Long memberId, Long restaurantId);

	Optional<MemberFavoriteRestaurant> findByMemberIdAndRestaurantIdAndDeletedAtIsNull(Long memberId,
		Long restaurantId);
}
