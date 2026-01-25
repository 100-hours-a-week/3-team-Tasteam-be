package com.tasteam.domain.subgroup.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.subgroup.entity.SubgroupMember;

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
}
