package com.tasteam.domain.restaurant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.RestaurantImage;
import com.tasteam.domain.restaurant.repository.projection.RestaurantImageProjection;

public interface RestaurantImageRepository extends JpaRepository<RestaurantImage, Long> {

	List<RestaurantImage> findByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(Long restaurantId);

	List<RestaurantImage> findByRestaurantIdAndDeletedAtIsNull(Long restaurantId);

	@Query("""
		select
		    ri.restaurant.id as restaurantId,
		    ri.id as imageId,
		    ri.imageUrl as imageUrl
		from RestaurantImage ri
		where ri.restaurant.id in :restaurantIds
		  and ri.deletedAt is null
		order by ri.restaurant.id asc, ri.sortOrder asc
		""")
	List<RestaurantImageProjection> findRestaurantImages(
		@Param("restaurantIds")
		List<Long> restaurantIds);

	void findAllByRestaurantId(long restaurantId);
}
