package com.tasteam.domain.restaurant.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.FoodCategory;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

	long countByNameIn(Collection<String> categories);

	long countByIdIn(Collection<Long> categories);
}
