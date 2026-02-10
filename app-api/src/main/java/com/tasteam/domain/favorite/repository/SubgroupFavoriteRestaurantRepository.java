package com.tasteam.domain.favorite.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.favorite.dto.FavoriteCountBySubgroupDto;
import com.tasteam.domain.favorite.entity.SubgroupFavoriteRestaurant;

public interface SubgroupFavoriteRestaurantRepository extends JpaRepository<SubgroupFavoriteRestaurant, Long> {

	Optional<SubgroupFavoriteRestaurant> findBySubgroupIdAndRestaurantId(Long subgroupId, Long restaurantId);

	boolean existsBySubgroupIdAndRestaurantId(Long subgroupId, Long restaurantId);

	boolean existsByRestaurantIdAndMemberId(Long restaurantId, Long memberId);

	void deleteBySubgroupIdAndRestaurantId(Long subgroupId, Long restaurantId);

	long countBySubgroupId(Long subgroupId);

	@Query("""
		select new com.tasteam.domain.favorite.dto.FavoriteCountBySubgroupDto(
			sfr.subgroupId,
			count(sfr.id)
		)
		from SubgroupFavoriteRestaurant sfr
		where sfr.subgroupId in :subgroupIds
		group by sfr.subgroupId
		""")
	List<FavoriteCountBySubgroupDto> countBySubgroupIds(
		@Param("subgroupIds")
		List<Long> subgroupIds);

	@Query("""
		select sfr.subgroupId
		from SubgroupFavoriteRestaurant sfr
		where sfr.subgroupId in :subgroupIds
			and sfr.restaurantId = :restaurantId
		""")
	List<Long> findFavoritedSubgroupIds(
		@Param("subgroupIds")
		List<Long> subgroupIds,
		@Param("restaurantId")
		Long restaurantId);
}
