package com.tasteam.domain.analytics.persistence;

import java.util.List;

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
	private final List<UserActivityStoredHook> storedHooks;

	@Transactional
	public void store(ActivityEvent event) {
		boolean inserted = userActivityEventJdbcRepository.insertIgnoreDuplicate(event);
		if (!inserted) {
			log.info("중복 사용자 이벤트를 감지하여 저장을 건너뜁니다. eventId={}, eventName={}",
				event.eventId(), event.eventName());
			return;
		}
		dispatchStoredHooks(event);
	}

	private void dispatchStoredHooks(ActivityEvent event) {
		if (storedHooks.isEmpty()) {
			return;
		}

		for (UserActivityStoredHook hook : storedHooks) {
			try {
				hook.afterStored(event);
			} catch (Exception ex) {
				log.error("사용자 이벤트 저장 후 hook 실행에 실패했습니다. hookType={}, eventId={}, eventName={}",
					resolveHookType(hook), event.eventId(), event.eventName(), ex);
			}
		}
	}

	private String resolveHookType(UserActivityStoredHook hook) {
		try {
			return hook.hookType();
		} catch (Exception ignored) {
			return hook.getClass().getSimpleName();
		}
	}
}
