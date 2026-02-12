package com.tasteam.domain.subgroup.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.favorite.dto.FavoriteSubgroupTargetRow;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.member.dto.response.MemberSubgroupDetailSummaryRow;
import com.tasteam.domain.member.dto.response.MemberSubgroupSummaryRow;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.domain.subgroup.entity.SubgroupMember;
import com.tasteam.domain.subgroup.type.SubgroupStatus;

public interface SubgroupMemberRepository extends JpaRepository<SubgroupMember, Long> {

	Optional<SubgroupMember> findBySubgroupIdAndMember_IdAndDeletedAtIsNull(Long subgroupId, Long memberId);

	Optional<SubgroupMember> findBySubgroupIdAndMember_Id(Long subgroupId, Long memberId);

	@Query("""
		select sm
		from SubgroupMember sm
		join Subgroup s on s.id = sm.subgroupId
		where sm.member.id = :memberId
			and sm.deletedAt is null
			and s.group.id = :groupId
			and s.deletedAt is null
		""")
	List<SubgroupMember> findActiveMembersByMemberAndGroup(
		@Param("groupId")
		Long groupId,
		@Param("memberId")
		Long memberId);

	@Query("""
		select new com.tasteam.domain.member.dto.response.MemberSubgroupSummaryRow(
			g.id,
			s.id,
			s.name
		)
		from SubgroupMember sm
		join Subgroup s on s.id = sm.subgroupId
		join s.group g
		where sm.member.id = :memberId
			and sm.deletedAt is null
			and s.deletedAt is null
			and s.status = :activeSubgroupStatus
			and g.deletedAt is null
			and g.status = :activeGroupStatus
		order by g.id asc, s.id asc
		""")
	List<MemberSubgroupSummaryRow> findMemberSubgroupSummaries(
		@Param("memberId")
		Long memberId,
		@Param("activeSubgroupStatus")
		SubgroupStatus activeSubgroupStatus,
		@Param("activeGroupStatus")
		GroupStatus activeGroupStatus);

	@Query("""
		select new com.tasteam.domain.member.dto.response.MemberSubgroupDetailSummaryRow(
			g.id,
			s.id,
			s.name,
			s.memberCount,
			s.profileImageUrl
		)
		from SubgroupMember sm
		join Subgroup s on s.id = sm.subgroupId
		join s.group g
		where sm.member.id = :memberId
			and sm.deletedAt is null
			and s.deletedAt is null
			and s.status = :activeSubgroupStatus
			and g.deletedAt is null
			and g.status = :activeGroupStatus
		order by g.name asc, s.name asc
		""")
	List<MemberSubgroupDetailSummaryRow> findMemberSubgroupDetailSummaries(
		@Param("memberId")
		Long memberId,
		@Param("activeSubgroupStatus")
		SubgroupStatus activeSubgroupStatus,
		@Param("activeGroupStatus")
		GroupStatus activeGroupStatus);

	@Query("""
		select new com.tasteam.domain.subgroup.dto.SubgroupMemberListItem(
			sm.id,
			sm.member.id,
			m.nickname,
			m.profileImageUrl,
			sm.createdAt
		)
		from SubgroupMember sm
		join sm.member m
		where sm.subgroupId = :subgroupId
			and sm.deletedAt is null
			and (:cursor is null or sm.id < :cursor)
		order by sm.id desc
		""")
	List<SubgroupMemberListItem> findSubgroupMembers(
		@Param("subgroupId")
		Long subgroupId,
		@Param("cursor")
		Long cursor,
		org.springframework.data.domain.Pageable pageable);

	@Query("""
		select new com.tasteam.domain.favorite.dto.FavoriteSubgroupTargetRow(
			s.id,
			s.name
		)
		from SubgroupMember sm
		join Subgroup s on s.id = sm.subgroupId
		join s.group g
		where sm.member.id = :memberId
			and sm.deletedAt is null
			and s.deletedAt is null
			and s.status = :activeSubgroupStatus
			and g.deletedAt is null
			and g.status = :activeGroupStatus
		order by s.name desc, s.id desc
		""")
	List<FavoriteSubgroupTargetRow> findFavoriteSubgroupTargets(
		@Param("memberId")
		Long memberId,
		@Param("activeSubgroupStatus")
		SubgroupStatus activeSubgroupStatus,
		@Param("activeGroupStatus")
		GroupStatus activeGroupStatus);
}
