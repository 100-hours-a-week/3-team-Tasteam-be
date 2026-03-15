package com.tasteam.domain.restaurant.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	Optional<Restaurant> findByIdAndDeletedAtIsNull(Long id);

	/**
	 * ID 목록 중 삭제되지 않은 레스토랑만 조회. 벡터 업로드 배치 대상 조회용.
	 */
	List<Restaurant> findByIdInAndDeletedAtIsNull(Iterable<Long> ids);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	/**
	 * vector_epoch가 expectedEpoch일 때만 1 증가 및 vector_synced_at 갱신.
	 * 다른 트랜잭션이 이미 올렸으면 0건 반환.
	 *
	 * @return 업데이트된 행 수 (0 또는 1)
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Restaurant r SET r.vectorEpoch = r.vectorEpoch + 1, r.vectorSyncedAt = :syncedAt WHERE r.id = :id AND r.vectorEpoch = :expectedEpoch")
	int incrementVectorEpochIfMatch(
		@Param("id")
		Long id,
		@Param("expectedEpoch")
		Long expectedEpoch,
		@Param("syncedAt")
		Instant syncedAt);

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

}
