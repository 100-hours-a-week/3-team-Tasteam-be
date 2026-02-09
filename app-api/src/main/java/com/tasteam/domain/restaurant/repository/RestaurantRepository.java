package com.tasteam.domain.restaurant.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	Optional<Restaurant> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	@Query("""
		select r
		from Restaurant r
		where r.deletedAt is null
		  and (
		    lower(r.name) like lower(concat('%', :keyword, '%'))
		    or lower(r.fullAddress) like lower(concat('%', :keyword, '%'))
		  )
		  and (
		    :cursorUpdatedAt is null
		    or r.updatedAt < :cursorUpdatedAt
		    or (r.updatedAt = :cursorUpdatedAt and r.id < :cursorId)
		  )
		order by r.updatedAt desc, r.id desc
		""")
	List<Restaurant> searchByKeyword(
		@Param("keyword")
		String keyword,
		@Param("cursorUpdatedAt")
		Instant cursorUpdatedAt,
		@Param("cursorId")
		Long cursorId,
		Pageable pageable);

	@Query(value = """
		select
		  r.id as id,
		  r.name as name,
		  ST_Distance(
		    r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
		  ) as distanceMeter
		from restaurant r
		where r.deleted_at is null
		  and ST_DWithin(
		    r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
		    :radiusMeter
		  )
		order by distanceMeter asc, r.id asc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findNearbyRestaurants(
		@Param("latitude")
		double latitude,
		@Param("longitude")
		double longitude,
		@Param("radiusMeter")
		int radiusMeter,
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name,
		  ST_Distance(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distanceMeter
		from restaurant r
		left join review rv on rv.restaurant_id = r.id and rv.deleted_at is null
		where r.deleted_at is null
		  and ST_DWithin(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeter)
		group by r.id
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
		@Param("limit")
		int limit);

	@Query(value = """
		select r.id as id, r.name as name,
		  ST_Distance(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distanceMeter
		from restaurant r
		where r.deleted_at is null
		  and ST_DWithin(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeter)
		order by r.created_at desc, r.id desc
		limit :limit
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
		select r.id as id, r.name as name,
		  ST_Distance(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distanceMeter
		from restaurant r
		join ai_restaurant_review_analysis a on a.restaurant_id = r.id
		where r.deleted_at is null
		  and ST_DWithin(r.location::geography,
		    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeter)
		order by a.positive_ratio desc, r.id asc
		limit :limit
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
		join ai_restaurant_review_analysis a on a.restaurant_id = r.id
		where r.deleted_at is null
		  and r.id not in (:excludeIds)
		order by a.positive_ratio desc, r.id asc
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
}
