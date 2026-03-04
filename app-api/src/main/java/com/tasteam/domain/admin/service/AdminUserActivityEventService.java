package com.tasteam.domain.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.request.AdminUserActivityEventSearchCondition;
import com.tasteam.domain.admin.dto.response.AdminUserActivityEventListItem;
import com.tasteam.domain.analytics.persistence.UserActivityEventJpaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserActivityEventService {

	private final UserActivityEventJpaRepository repository;

	@Transactional(readOnly = true)
	public Page<AdminUserActivityEventListItem> getEvents(
		AdminUserActivityEventSearchCondition condition, Pageable pageable) {
		return repository.findByCondition(condition, pageable)
			.map(AdminUserActivityEventListItem::from);
	}
}
