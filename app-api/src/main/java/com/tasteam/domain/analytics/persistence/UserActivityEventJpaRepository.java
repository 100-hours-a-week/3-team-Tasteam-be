package com.tasteam.domain.analytics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityEventJpaRepository
	extends JpaRepository<UserActivityEventEntity, Long>, UserActivityEventQueryRepository {}
