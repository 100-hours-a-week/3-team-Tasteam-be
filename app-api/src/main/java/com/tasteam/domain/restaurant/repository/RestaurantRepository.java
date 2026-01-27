package com.tasteam.domain.restaurant.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	Optional<Restaurant> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	@Query(value = """
		select *
		from restaurant r
		where r.deleted_at is null
		  and (
		    lower(r.name) like lower(concat('%', :keyword, '%'))
		    or lower(r.full_address) like lower(concat('%', :keyword, '%'))
		  )
		  and (
		    cast(:cursorUpdatedAt as timestamptz) is null
		    or r.updated_at < :cursorUpdatedAt
		    or (r.updated_at = :cursorUpdatedAt and r.id < :cursorId)
		  )
		order by r.updated_at desc, r.id desc
		""", nativeQuery = true)
	List<Restaurant> searchByKeyword(
		@Param("keyword")
		String keyword,
		@Param("cursorUpdatedAt")
		Instant cursorUpdatedAt,
		@Param("cursorId")
		Long cursorId,
		Pageable pageable);
}
