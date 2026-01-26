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
		    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
		  ) as distanceMeter
		from restaurant r
		where r.deleted_at is null
		  and ST_DWithin(
		    r.location::geography,
		    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
		    :radiusMeter
		  )
		order by distanceMeter asc, r.id asc
		limit :limit
		""", nativeQuery = true)
	List<MainRestaurantDistanceProjection> findNearbyRestaurants(
		@Param("lat")
		double lat,
		@Param("lng")
		double lng,
		@Param("radiusMeter")
		int radiusMeter,
		@Param("limit")
		int limit);
}
