package com.tasteam.domain.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.group.entity.GroupAuthCode;

public interface GroupAuthCodeRepository extends JpaRepository<GroupAuthCode, Long> {

	java.util.Optional<GroupAuthCode> findByGroupId(Long groupId);
}
