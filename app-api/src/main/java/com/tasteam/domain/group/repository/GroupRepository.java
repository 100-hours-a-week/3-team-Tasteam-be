package com.tasteam.domain.group.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {

	Optional<Group> findByIdAndDeletedAtIsNull(Long id);
}
