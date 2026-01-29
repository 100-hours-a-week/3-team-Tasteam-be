package com.tasteam.domain.restaurant.repository.impl;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.tasteam.domain.restaurant.dto.RestaurantCursor;
import com.tasteam.domain.restaurant.dto.RestaurantDistanceQueryDto;
import com.tasteam.domain.restaurant.repository.RestaurantQueryRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class RestaurantQueryRepositoryImpl implements RestaurantQueryRepository {

	private final NamedParameterJdbcTemplate namedJdbcTemplate;

	@Override
	public List<RestaurantDistanceQueryDto> findRestaurantsWithDistance(
		double latitude,
		double longitude,
		double radiusMeter,
		Set<String> categories,
		RestaurantCursor cursor,
		int pageSize) {
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
			JOIN restaurant_food_category rfc
			     ON rfc.restaurant_id = r.id
			JOIN food_category fc
			     ON fc.id = rfc.food_category_id
			         AND fc.name IN (:categories)
			WHERE r.deleted_at IS NULL
			AND ST_DWithin(
			  r.location::geography,
			  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
			  :radiusMeter
			)
			-- 커서 조건 (cursor != null 인 경우만)
			AND (
			  :cursorDistance IS NULL
			  OR (
			    ST_Distance(
			      r.location::geography,
			      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
			    ) > :cursorDistance
			    OR (
			      ST_Distance(
			        r.location::geography,
			        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
			      ) = :cursorDistance
			      AND r.id > :cursorId
			    )
			  )
			)
			ORDER BY
			  distance_meter ASC,
			  r.id ASC
			LIMIT :pageSize
			""";

		Map<String, Object> params = new HashMap<>();
		params.put("latitude", latitude);
		params.put("longitude", longitude);
		params.put("radiusMeter", radiusMeter);
		params.put("categories", categories);
		params.put("cursorDistance", cursor == null ? null : cursor.distanceMeter());
		params.put("cursorId", cursor == null ? null : cursor.id());
		params.put("pageSize", pageSize);

		return namedJdbcTemplate.query(
			sql,
			params,
			(rs, rowNum) -> new RestaurantDistanceQueryDto(
				rs.getLong("id"),
				rs.getString("name"),
				rs.getString("full_address"),
				rs.getDouble("distance_meter")));
	}

	@Override
	public List<RestaurantDistanceQueryDto> findRestaurantsWithDistance(
		Long groupId,
		double latitude,
		double longitude,
		double radiusMeter,
		Set<String> categories,
		RestaurantCursor cursor,
		int pageSize) {
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
			JOIN restaurant_food_category rfc
			     ON rfc.restaurant_id = r.id
			JOIN food_category fc
			     ON fc.id = rfc.food_category_id
			         AND fc.name IN (:categories)
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
			-- 커서 조건 (cursor != null 인 경우만)
			AND (
			  :cursorDistance IS NULL
			  OR (
			    ST_Distance(
			      r.location::geography,
			      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
			    ) > :cursorDistance
			    OR (
			      ST_Distance(
			        r.location::geography,
			        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
			      ) = :cursorDistance
			      AND r.id > :cursorId
			    )
			  )
			)
			ORDER BY
			  distance_meter ASC,
			  r.id ASC
			LIMIT :pageSize
			""";

		Map<String, Object> params = new HashMap<>();
		params.put("latitude", latitude);
		params.put("longitude", longitude);
		params.put("radiusMeter", radiusMeter);
		params.put("categories", categories);
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
				rs.getString("full_address"),
				rs.getDouble("distance_meter")));
	}
}
