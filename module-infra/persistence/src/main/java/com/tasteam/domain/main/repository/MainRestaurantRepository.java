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
		group by r.id, r.name, r.location
		order by count(rv.id) desc, r.id asc
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
		where r.deleted_at is null
		  and r.id not in (:excludeIds)
		group by r.id
		order by count(rv.id) desc, r.id asc
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
