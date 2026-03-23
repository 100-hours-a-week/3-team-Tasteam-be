package com.tasteam.domain.restaurant.entity;

import org.hibernate.annotations.Comment;

import jakarta.persistence.*;
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
@Table(name = "restaurant_food_category", uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id",
	"food_category_id"}))
@Comment("음식점과 음식 카테고리 간의 매핑 테이블")
public class RestaurantFoodCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "food_category_id", nullable = false)
	private FoodCategory foodCategory;

	public static RestaurantFoodCategory create(Restaurant restaurant, FoodCategory foodCategory) {
		return RestaurantFoodCategory.builder()
			.restaurant(restaurant)
			.foodCategory(foodCategory)
			.build();
	}
}
