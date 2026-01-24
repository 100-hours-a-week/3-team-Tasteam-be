package com.tasteam.domain.subgroup.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.subgroup.entity.SubgroupMember;

public interface SubgroupMemberRepository extends JpaRepository<SubgroupMember, Long> {

	Optional<SubgroupMember> findBySubgroupIdAndMember_IdAndDeletedAtIsNull(Long subgroupId, Long memberId);

	Optional<SubgroupMember> findBySubgroupIdAndMember_Id(Long subgroupId, Long memberId);
}
