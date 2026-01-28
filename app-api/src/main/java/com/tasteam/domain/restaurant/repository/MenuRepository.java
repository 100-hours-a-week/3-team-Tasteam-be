package com.tasteam.domain.restaurant.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

	@Query("""
		SELECT m FROM Menu m
		WHERE m.category.id IN :categoryIds
		ORDER BY m.category.id ASC, m.displayOrder ASC, m.id ASC
		""")
	List<Menu> findByCategoryIdsOrderByDisplayOrder(
		@Param("categoryIds")
		Set<Long> categoryIds);

	@Query("""
		SELECT m FROM Menu m
		WHERE m.category.id IN :categoryIds
		ORDER BY m.category.id ASC, m.isRecommended DESC, m.displayOrder ASC, m.id ASC
		""")
	List<Menu> findByCategoryIdsOrderByRecommendedAndDisplayOrder(
		@Param("categoryIds")
		Set<Long> categoryIds);
}
