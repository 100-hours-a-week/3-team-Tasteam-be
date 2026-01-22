package com.tasteam.domain.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.group.dto.GroupMemberListItem;
import com.tasteam.domain.group.entity.GroupMember;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

	Optional<GroupMember> findByGroupIdAndMember_IdAndDeletedAtIsNull(Long groupId, Long memberId);

	Optional<GroupMember> findByGroupIdAndMember_Id(Long groupId, Long memberId);

	@Query("""
		select new com.tasteam.domain.group.dto.GroupMemberListItem(
			gm.id,
			gm.member.id,
			m.nickname,
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
}
