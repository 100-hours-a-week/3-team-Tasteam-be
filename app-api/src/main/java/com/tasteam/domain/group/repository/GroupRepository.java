package com.tasteam.domain.group.repository;

import java.util.Optional;

import com.tasteam.domain.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {

	Optional<Group> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByNameAndDeletedAtIsNull(String name);
}
