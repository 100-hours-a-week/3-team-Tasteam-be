package com.tasteam.domain.main.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.group.repository.projection.GroupLocationProjection;

public interface MainGroupRepository extends Repository<GroupMember, Long> {

	@Query(value = """
		SELECT ST_Y(g.location) as latitude, ST_X(g.location) as longitude
		FROM group_member gm
		JOIN "group" g ON g.id = gm.group_id
		WHERE gm.member_id = :memberId
		  AND gm.deleted_at IS NULL
		  AND g.deleted_at IS NULL
		  AND g.status = 'ACTIVE'
		  AND g.location IS NOT NULL
		ORDER BY gm.id DESC
		LIMIT 1
		""", nativeQuery = true)
	Optional<GroupLocationProjection> findFirstGroupLocationByMemberId(
		@Param("memberId")
		Long memberId);
}
