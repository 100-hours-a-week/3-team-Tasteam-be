package com.tasteam.domain.restaurant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

	@Query("""
		SELECT mc FROM MenuCategory mc
		WHERE mc.restaurant.id = :restaurantId
		ORDER BY mc.displayOrder ASC, mc.id ASC
		""")
	List<MenuCategory> findByRestaurantIdOrderByDisplayOrder(
		@Param("restaurantId")
		Long restaurantId);
}
