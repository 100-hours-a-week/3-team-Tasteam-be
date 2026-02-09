package com.tasteam.domain.restaurant.repository.impl;

import static com.tasteam.domain.restaurant.entity.QRestaurant.restaurant;
import static com.tasteam.domain.restaurant.entity.QRestaurantFoodCategory.restaurantFoodCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tasteam.domain.admin.dto.request.AdminRestaurantSearchCondition;
import com.tasteam.domain.restaurant.dto.RestaurantCursor;
import com.tasteam.domain.restaurant.dto.RestaurantDistanceQueryDto;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantQueryRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class RestaurantQueryRepositoryImpl implements RestaurantQueryRepository {

	private final NamedParameterJdbcTemplate namedJdbcTemplate;
	private final JPAQueryFactory queryFactory;

	@Override
	public List<RestaurantDistanceQueryDto> findRestaurantsWithDistance(
		double latitude,
		double longitude,
		double radiusMeter,
		Set<String> categories,
		RestaurantCursor cursor,
		int pageSize) {
		boolean hasCategories = categories != null && !categories.isEmpty();

		String sql = """
			SELECT
			  r.id AS id,
			  r.name AS name,
			  r.full_address AS address,
			  ST_Distance(
			    r.location::geography,
			    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
			  ) AS distance_meter
			FROM restaurant r
			%s
			WHERE r.deleted_at IS NULL
			AND ST_DWithin(
			  r.location::geography,
			  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
			  :radiusMeter
			)
			-- 커서 조건 (cursor != null 인 경우만)
			AND (
			  CAST(:cursorDistance AS double precision) IS NULL
			  OR (
			    ST_Distance(
			      r.location::geography,
			      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
			    ) > CAST(:cursorDistance AS double precision)
			    OR (
			      ST_Distance(
			        r.location::geography,
			        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
			      ) = CAST(:cursorDistance AS double precision)
			      AND r.id > CAST(:cursorId AS bigint)
			    )
			  )
			)
			ORDER BY
			  distance_meter ASC,
			  r.id ASC
			LIMIT :pageSize
			""".formatted(hasCategories ? """
			JOIN restaurant_food_category rfc
			     ON rfc.restaurant_id = r.id
			JOIN food_category fc
			     ON fc.id = rfc.food_category_id
			         AND fc.name IN (:categories)
			""" : "");

		Map<String, Object> params = new HashMap<>();
		params.put("latitude", latitude);
		params.put("longitude", longitude);
		params.put("radiusMeter", radiusMeter);
		if (hasCategories) {
			params.put("categories", categories);
		}
		params.put("cursorDistance", cursor == null ? null : cursor.distanceMeter());
		params.put("cursorId", cursor == null ? null : cursor.id());
		params.put("pageSize", pageSize);

		return namedJdbcTemplate.query(
			sql,
			params,
			(rs, rowNum) -> new RestaurantDistanceQueryDto(
				rs.getLong("id"),
				rs.getString("name"),
				rs.getString("address"),
				rs.getDouble("distance_meter")));
	}

	@Override
	public List<RestaurantDistanceQueryDto> findRestaurantsWithDistance(
		Long groupId,
		double latitude,
		double longitude,
		int radiusMeter,
		Set<String> categories,
		RestaurantCursor cursor,
		int pageSize) {
		boolean hasCategories = categories != null && !categories.isEmpty();

		String sql = """
			WITH ranked AS (
			  SELECT
				r.id,
				r.name,
				r.full_address,
				ST_Distance(
				  r.location::geography,
				  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
				) AS distance_meter
			  FROM restaurant r
			  %s
			  WHERE r.deleted_at IS NULL
				AND ST_DWithin(
				  r.location::geography,
				  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
				  :radiusMeter
				)
				AND EXISTS (
				  SELECT 1
				  FROM review rv
				  WHERE rv.restaurant_id = r.id
					AND rv.group_id = :groupId
				)
			)
			SELECT *
			FROM ranked
			WHERE (
			  CAST(:cursorDistance AS double precision) IS NULL
			  OR (
				distance_meter > CAST(:cursorDistance AS double precision)
				OR (
				  distance_meter = CAST(:cursorDistance AS double precision)
				  AND id > CAST(:cursorId AS bigint)
				)
			  )
			)
			ORDER BY distance_meter ASC, id ASC
			LIMIT :pageSize
			""".formatted(
			hasCategories ? """
					JOIN restaurant_food_category rfc
						 ON rfc.restaurant_id = r.id
					JOIN food_category fc
						 ON fc.id = rfc.food_category_id
						AND fc.name IN (:categories)
				""" : "");

		Map<String, Object> params = new HashMap<>();
		params.put("latitude", latitude);
		params.put("longitude", longitude);
		params.put("radiusMeter", radiusMeter);
		if (hasCategories) {
			params.put("categories", categories);
		}
		params.put("groupId", groupId);
		params.put("cursorDistance", cursor == null ? null : cursor.distanceMeter());
		params.put("cursorId", cursor == null ? null : cursor.id());
		params.put("pageSize", pageSize);

		return namedJdbcTemplate.query(
			sql,
			params,
			(rs, rowNum) -> new RestaurantDistanceQueryDto(
				rs.getLong("id"),
				rs.getString("name"),
				rs.getString("address"),
				rs.getDouble("distance_meter")));
	}

	@Override
	public Page<Restaurant> findAllByAdminCondition(
		AdminRestaurantSearchCondition condition,
		Pageable pageable) {

		BooleanBuilder builder = new BooleanBuilder();

		if (condition.name() != null && !condition.name().isBlank()) {
			builder.and(restaurant.name.contains(condition.name()));
		}

		if (condition.address() != null && !condition.address().isBlank()) {
			builder.and(restaurant.fullAddress.contains(condition.address()));
		}

		if (condition.foodCategoryId() != null) {
			builder.and(restaurant.id.in(
				queryFactory
					.select(restaurantFoodCategory.restaurant.id)
					.from(restaurantFoodCategory)
					.where(restaurantFoodCategory.foodCategory.id.eq(condition.foodCategoryId()))));
		}

		if (condition.isDeleted() != null) {
			if (condition.isDeleted()) {
				builder.and(restaurant.deletedAt.isNotNull());
			} else {
				builder.and(restaurant.deletedAt.isNull());
			}
		}

		JPAQuery<Restaurant> query = queryFactory
			.selectFrom(restaurant)
			.where(builder)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize());

		if (pageable.getSort().isSorted()) {
			pageable.getSort().forEach(order -> {
				if (order.getProperty().equals("createdAt")) {
					query.orderBy(order.isAscending() ? restaurant.createdAt.asc() : restaurant.createdAt.desc());
				} else if (order.getProperty().equals("name")) {
					query.orderBy(order.isAscending() ? restaurant.name.asc() : restaurant.name.desc());
				}
			});
		} else {
			query.orderBy(restaurant.createdAt.desc());
		}

		List<Restaurant> content = query.fetch();

		long total = queryFactory
			.select(restaurant.count())
			.from(restaurant)
			.where(builder)
			.fetchOne();

		return new PageImpl<>(content, pageable, total);
	}
}
