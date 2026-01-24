package com.tasteam.domain.subgroup.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.type.SubgroupStatus;

public interface SubgroupRepository extends JpaRepository<Subgroup, Long> {
	boolean existsByIdAndStatus(Long id, SubgroupStatus status);
}
