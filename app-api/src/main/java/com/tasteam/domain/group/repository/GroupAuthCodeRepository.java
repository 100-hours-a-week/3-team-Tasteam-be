package com.tasteam.domain.group.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.group.entity.GroupAuthCode;

public interface GroupAuthCodeRepository extends JpaRepository<GroupAuthCode, Long> {

	boolean existsByGroupIdAndExpiresAtAfterAndVerifiedAtIsNull(Long groupId, Instant now);

	java.util.Optional<GroupAuthCode> findByGroupIdAndCodeAndExpiresAtAfterAndVerifiedAtIsNull(
		Long groupId,
		String code,
		Instant now);

	java.util.Optional<GroupAuthCode> findByGroupId(Long groupId);
}
