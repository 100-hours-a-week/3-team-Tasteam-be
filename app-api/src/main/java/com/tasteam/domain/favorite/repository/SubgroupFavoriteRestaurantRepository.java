package com.tasteam.domain.favorite.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.favorite.entity.SubgroupFavoriteRestaurant;

public interface SubgroupFavoriteRestaurantRepository extends JpaRepository<SubgroupFavoriteRestaurant, Long> {

	Optional<SubgroupFavoriteRestaurant> findBySubgroupIdAndRestaurantId(Long subgroupId, Long restaurantId);

	boolean existsBySubgroupIdAndRestaurantId(Long subgroupId, Long restaurantId);

	boolean existsByRestaurantIdAndMemberId(Long restaurantId, Long memberId);

	void deleteBySubgroupIdAndRestaurantId(Long subgroupId, Long restaurantId);
}
