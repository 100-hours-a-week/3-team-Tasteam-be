package com.tasteam.domain.main.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantLocationProjection;

public interface MainRestaurantRepository extends Repository<Restaurant, Long> {

	@Query(value = """
		with nearby as (
		  select r.id
		  from restaurant r
		  where r.deleted_at is null
		    and ST_DWithin(r.location::geography,
		      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeter)
		  order by r.location::geography <-> ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
		  limit :candidateLimit
		)
		select r.id as id, r.name as name,
		  ST_Distance(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distanceMeter
		from restaurant r
		join nearby n on n.id = r.id
		left join review rv on rv.restaurant_id = r.id and rv.deleted_at is null
		left join restaurant_review_sentiment s on s.restaurant_id = r.id
		group by r.id, r.name, r.location, s.positive_percent
		order by case when s.positive_percent is null then 1 else 0 end asc,
		  coalesce(s.positive_percent, 0) desc,
		  count(rv.id) desc,
		  r.id asc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findHotRestaurants(
		@Param("latitude")
		double latitude,
		@Param("longitude")
		double longitude,
		@Param("radiusMeter")
		int radiusMeter,
		@Param("candidateLimit")
		int candidateLimit,
		@Param("limit")
		int limit);

	@Query(value = """
		with ranked as (
		  select r.id, r.name, r.location
		  from restaurant r
		  where r.deleted_at is null
		    and ST_DWithin(r.location::geography,
		      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeter)
		  order by r.created_at desc, r.id desc
		  limit :limit
		)
		select id, name,
		  ST_Distance(location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distanceMeter
		from ranked
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findNewRestaurants(
		@Param("latitude")
		double latitude,
		@Param("longitude")
		double longitude,
		@Param("radiusMeter")
		int radiusMeter,
		@Param("limit")
		int limit);

	@Query(value = """
		with ranked as (
		  select r.id, r.name, r.location
		  from restaurant r
		  join restaurant_review_sentiment s on s.restaurant_id = r.id
		  where r.deleted_at is null
		    and ST_DWithin(r.location::geography,
		      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeter)
		  order by s.positive_percent desc, r.id asc
		  limit :limit
		)
		select id, name,
		  ST_Distance(location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distanceMeter
		from ranked
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findAiRecommendRestaurants(
		@Param("latitude")
		double latitude,
		@Param("longitude")
		double longitude,
		@Param("radiusMeter")
		int radiusMeter,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name, cast(null as double precision) as distanceMeter
		from restaurant r
		left join review rv on rv.restaurant_id = r.id and rv.deleted_at is null
		left join restaurant_review_sentiment s on s.restaurant_id = r.id
		where r.deleted_at is null
		  and r.id not in (:excludeIds)
		group by r.id, r.name, s.positive_percent
		order by case when s.positive_percent is null then 1 else 0 end asc,
		  coalesce(s.positive_percent, 0) desc,
		  count(rv.id) desc,
		  r.id asc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findHotRestaurantsAll(
		@Param("excludeIds")
		List<Long> excludeIds,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name, cast(null as double precision) as distanceMeter
		from restaurant r
		where r.deleted_at is null
		  and r.id not in (:excludeIds)
		order by r.created_at desc, r.id desc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findNewRestaurantsAll(
		@Param("excludeIds")
		List<Long> excludeIds,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name, cast(null as double precision) as distanceMeter
		from restaurant r
		join restaurant_review_sentiment s on s.restaurant_id = r.id
		where r.deleted_at is null
		  and r.id not in (:excludeIds)
		order by s.positive_percent desc, r.id asc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findAiRecommendRestaurantsAll(
		@Param("excludeIds")
		List<Long> excludeIds,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name,
		  ST_Distance(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distanceMeter
		from restaurant r
		join restaurant_food_category rfc on rfc.restaurant_id = r.id
		join food_category fc on fc.id = rfc.food_category_id
		where r.deleted_at is null
		  and lower(fc.name) = lower(:categoryName)
		  and ST_DWithin(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeter)
		order by r.location::geography <-> ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
		  r.id asc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findDistanceRestaurantsByCategory(
		@Param("latitude")
		double latitude,
		@Param("longitude")
		double longitude,
		@Param("radiusMeter")
		int radiusMeter,
		@Param("categoryName")
		String categoryName,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name,
		  ST_Distance(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distanceMeter
		from restaurant r
		join restaurant_food_category rfc on rfc.restaurant_id = r.id
		join food_category fc on fc.id = rfc.food_category_id
		left join review rv on rv.restaurant_id = r.id and rv.deleted_at is null
		left join restaurant_review_sentiment s on s.restaurant_id = r.id
		where r.deleted_at is null
		  and lower(fc.name) = lower(:categoryName)
		  and ST_DWithin(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeter)
		group by r.id, r.name, r.location, s.positive_percent
		order by case when s.positive_percent is null then 1 else 0 end asc,
		  coalesce(s.positive_percent, 0) desc,
		  count(rv.id) desc,
		  ST_Distance(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) asc,
		  r.id asc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findHotRestaurantsByCategory(
		@Param("latitude")
		double latitude,
		@Param("longitude")
		double longitude,
		@Param("radiusMeter")
		int radiusMeter,
		@Param("categoryName")
		String categoryName,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name, cast(null as double precision) as distanceMeter
		from restaurant r
		join restaurant_food_category rfc on rfc.restaurant_id = r.id
		join food_category fc on fc.id = rfc.food_category_id
		left join review rv on rv.restaurant_id = r.id and rv.deleted_at is null
		left join restaurant_review_sentiment s on s.restaurant_id = r.id
		where r.deleted_at is null
		  and lower(fc.name) = lower(:categoryName)
		group by r.id, r.name, s.positive_percent
		order by case when s.positive_percent is null then 1 else 0 end asc,
		  coalesce(s.positive_percent, 0) desc,
		  count(rv.id) desc,
		  r.id asc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findHotRestaurantsAllByCategory(
		@Param("categoryName")
		String categoryName,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name, cast(null as double precision) as distanceMeter
		from restaurant r
		where r.deleted_at is null
		  and r.id not in (:excludeIds)
		order by random()
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findRandomRestaurants(
		@Param("excludeIds")
		List<Long> excludeIds,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name,
		    ST_Distance(
		        r.location::geography,
		        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
		    ) as distanceMeter
		from restaurant r
		where r.id in (:ids)
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findDistancesByIds(
		@Param("ids")
		List<Long> ids,
		@Param("latitude")
		double latitude,
		@Param("longitude")
		double longitude);

	@Query(value = """
		select r.id as id, r.name as name, cast(null as double precision) as distanceMeter
		from restaurant r
		where r.id in (:ids)
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findRestaurantsByIds(
		@Param("ids")
		List<Long> ids);

	@Query(value = """
		select r.id as id, r.name as name,
		    ST_Y(r.location::geometry) as latitude,
		    ST_X(r.location::geometry) as longitude
		from restaurant r
		where r.id in (:ids)
		  and r.location is not null
		""", nativeQuery = true)
	List<RestaurantLocationProjection> findLocationsByIds(
		@Param("ids")
		List<Long> ids);
}
