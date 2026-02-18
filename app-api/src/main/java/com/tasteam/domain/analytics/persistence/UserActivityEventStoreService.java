package com.tasteam.domain.analytics.persistence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.analytics.api.ActivityEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityEventStoreService {

	private final UserActivityEventJdbcRepository userActivityEventJdbcRepository;

	@Transactional
	public void store(ActivityEvent event) {
		boolean inserted = userActivityEventJdbcRepository.insertIgnoreDuplicate(event);
		if (!inserted) {
			log.info("중복 사용자 이벤트를 감지하여 저장을 건너뜁니다. eventId={}, eventName={}",
				event.eventId(), event.eventName());
		}
	}
}
