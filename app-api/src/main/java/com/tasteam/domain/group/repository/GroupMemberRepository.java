package com.tasteam.domain.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.group.dto.GroupMemberListItem;
import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.group.repository.projection.GroupMemberCountProjection;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryRow;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

	Optional<GroupMember> findByGroupIdAndMember_IdAndDeletedAtIsNull(Long groupId, Long memberId);

	Optional<GroupMember> findByGroupIdAndMember_Id(Long groupId, Long memberId);

	@Query("""
		select new com.tasteam.domain.group.dto.GroupMemberListItem(
			gm.id,
			gm.member.id,
			m.nickname,
			m.profileImageUuid,
			m.profileImageUrl,
			gm.createdAt
		)
		from GroupMember gm
		join gm.member m
		where gm.groupId = :groupId
			and gm.deletedAt is null
			and (:cursor is null or gm.id < :cursor)
		order by gm.id desc
		""")
	List<GroupMemberListItem> findGroupMembers(
		@Param("groupId")
		Long groupId,
		@Param("cursor")
		Long cursor,
		Pageable pageable);

	@Query("""
		select
			gm.groupId as groupId,
			count(gm.id) as memberCount
		from GroupMember gm
		where gm.groupId in :groupIds
			and gm.deletedAt is null
		group by gm.groupId
		""")
	List<GroupMemberCountProjection> findMemberCounts(
		@Param("groupIds")
		List<Long> groupIds);

	long countByGroupIdAndDeletedAtIsNull(Long groupId);

	@Query("""
		select new com.tasteam.domain.member.dto.response.MemberGroupSummaryRow(
			g.id,
			g.name
		)
		from GroupMember gm
		join com.tasteam.domain.group.entity.Group g on g.id = gm.groupId
		where gm.member.id = :memberId
			and gm.deletedAt is null
			and g.deletedAt is null
			and g.status = :activeStatus
		order by gm.id desc
		""")
	List<MemberGroupSummaryRow> findMemberGroupSummaries(
		@Param("memberId")
		Long memberId,
		@Param("activeStatus")
		GroupStatus activeStatus);
}
