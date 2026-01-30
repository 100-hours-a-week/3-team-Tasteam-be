package com.tasteam.domain.subgroup.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.type.SubgroupStatus;

public interface SubgroupRepository extends JpaRepository<Subgroup, Long> {

	Optional<Subgroup> findByIdAndDeletedAtIsNull(Long id);

	Optional<Subgroup> findByIdAndGroup_IdAndStatusAndDeletedAtIsNull(Long id, Long groupId, SubgroupStatus status);

	boolean existsByIdAndStatus(Long id, SubgroupStatus status);

	boolean existsByGroup_IdAndNameAndDeletedAtIsNull(Long groupId, String name);

	boolean existsByGroup_IdAndNameAndDeletedAtIsNullAndIdNot(Long groupId, String name, Long id);

	@Query("""
		select new com.tasteam.domain.subgroup.dto.SubgroupListItem(
			s.id,
			s.name,
			s.description,
			s.memberCount,
			s.profileImageUrl,
			s.joinType,
			s.createdAt
		)
		from Subgroup s
		where s.group.id = :groupId
			and s.status = :status
			and s.deletedAt is null
			and (
				:cursorName is null
				or (s.name > :cursorName)
				or (s.name = :cursorName and s.id > :cursorId)
			)
		order by s.name asc, s.id asc
		""")
	List<SubgroupListItem> findSubgroupsByGroup(
		@Param("groupId")
		Long groupId,
		@Param("status")
		SubgroupStatus status,
		@Param("cursorName")
		String cursorName,
		@Param("cursorId")
		Long cursorId,
		Pageable pageable);

	@Query("""
		select new com.tasteam.domain.subgroup.dto.SubgroupListItem(
			s.id,
			s.name,
			s.description,
			s.memberCount,
			s.profileImageUrl,
			s.joinType,
			s.createdAt
		)
		from Subgroup s
		where s.group.id = :groupId
			and s.status = :status
			and s.deletedAt is null
			and (:keyword is null or s.name like %:keyword%)
			and (
				:cursorCount is null
				or (s.memberCount < :cursorCount)
				or (s.memberCount = :cursorCount and s.id > :cursorId)
			)
		order by s.memberCount desc, s.id asc
		""")
	List<SubgroupListItem> searchSubgroupsByGroup(
		@Param("groupId")
		Long groupId,
		@Param("status")
		SubgroupStatus status,
		@Param("keyword")
		String keyword,
		@Param("cursorCount")
		Integer cursorCount,
		@Param("cursorId")
		Long cursorId,
		PageRequest of);

	@Query("""
		select new com.tasteam.domain.subgroup.dto.SubgroupListItem(
			s.id,
			s.name,
			s.description,
			s.memberCount,
			s.profileImageUrl,
			s.createdAt
		)
		from SubgroupMember sm
		join Subgroup s on s.id = sm.subgroupId
		where sm.member.id = :memberId
			and sm.deletedAt is null
			and s.group.id = :groupId
			and s.deletedAt is null
			and (:keyword is null or s.name like %:keyword%)
			and (
				:cursorName is null
				or (s.name > :cursorName)
				or (s.name = :cursorName and s.id > :cursorId)
			)
		order by s.name asc, s.id asc
		""")
	List<SubgroupListItem> findMySubgroupsByGroup(
		@Param("groupId")
		Long groupId,
		@Param("memberId")
		Long memberId,
		@Param("keyword")
		String keyword,
		@Param("cursorName")
		String cursorName,
		@Param("cursorId")
		Long cursorId,
		Pageable pageable);
}
