package com.tasteam.domain.restaurant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.RestaurantFoodCategory;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;

public interface RestaurantFoodCategoryRepository extends JpaRepository<RestaurantFoodCategory, Long> {

	List<RestaurantFoodCategory> findByRestaurantId(Long restaurantId);

	long countByRestaurantId(Long restaurantId);

	@Query("""
			select
				rfc.restaurant.id as restaurantId,
				fc.name as categoryName
			from RestaurantFoodCategory rfc
			join rfc.foodCategory fc
			where rfc.restaurant.id in :restaurantIds
		""")
	List<RestaurantCategoryProjection> findCategoriesByRestaurantIds(
		List<Long> restaurantIds);

	@Modifying
	@Query("DELETE FROM RestaurantFoodCategory r WHERE r.restaurant.id = :restaurantId")
	void deleteByRestaurantId(@Param("restaurantId")
	Long restaurantId);
}
