package com.tasteam.domain.analytics.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tasteam.domain.admin.dto.request.AdminUserActivityEventSearchCondition;

public interface UserActivityEventQueryRepository {

	Page<UserActivityEventEntity> findByCondition(
		AdminUserActivityEventSearchCondition condition, Pageable pageable);
}
