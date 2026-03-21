package com.tasteam.domain.restaurant.repository;

import static com.tasteam.domain.restaurant.entity.QRestaurant.restaurant;
import static com.tasteam.domain.restaurant.entity.QRestaurantFoodCategory.restaurantFoodCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.tasteam.domain.admin.dto.request.AdminRestaurantSearchCondition;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.Restaurant;

@Repository
public class AdminRestaurantQueryRepository extends QueryDslSupport {

	public AdminRestaurantQueryRepository() {
		super(Restaurant.class);
	}

	public Page<Restaurant> findAllByAdminCondition(AdminRestaurantSearchCondition condition, Pageable pageable) {
		BooleanBuilder predicate = buildPredicate(condition);

		JPAQuery<Restaurant> contentQuery = getQueryFactory()
			.selectFrom(restaurant)
			.where(predicate)
			.orderBy(restaurant.id.desc());

		JPAQuery<Long> countQuery = getQueryFactory()
			.select(restaurant.count())
			.from(restaurant)
			.where(predicate);

		if (condition.foodCategoryId() != null) {
			contentQuery.join(restaurantFoodCategory).on(restaurantFoodCategory.restaurant.eq(restaurant));
			countQuery.join(restaurantFoodCategory).on(restaurantFoodCategory.restaurant.eq(restaurant));
		}

		return applyPagination(pageable, contentQuery, countQuery);
	}

	private BooleanBuilder buildPredicate(AdminRestaurantSearchCondition condition) {
		BooleanBuilder predicate = new BooleanBuilder();

		if (condition.name() != null && !condition.name().isBlank()) {
			predicate.and(restaurant.name.containsIgnoreCase(condition.name()));
		}
		if (condition.address() != null && !condition.address().isBlank()) {
			predicate.and(restaurant.fullAddress.containsIgnoreCase(condition.address()));
		}
		if (condition.foodCategoryId() != null) {
			predicate.and(restaurantFoodCategory.foodCategory.id.eq(condition.foodCategoryId()));
		}
		if (Boolean.TRUE.equals(condition.isDeleted())) {
			predicate.and(restaurant.deletedAt.isNotNull());
		} else if (Boolean.FALSE.equals(condition.isDeleted())) {
			predicate.and(restaurant.deletedAt.isNull());
		}

		return predicate;
	}
}
