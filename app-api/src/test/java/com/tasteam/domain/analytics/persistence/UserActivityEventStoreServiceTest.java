package com.tasteam.domain.analytics.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;

@UnitTest
@DisplayName("사용자 이벤트 저장 서비스")
class UserActivityEventStoreServiceTest {

	@Test
	@DisplayName("저장에 성공하면 등록된 저장 후 hook를 실행한다")
	void store_executesStoredHooksWhenInserted() {
		// given
		UserActivityEventJdbcRepository repository = mock(UserActivityEventJdbcRepository.class);
		UserActivityStoredHook storedHook = mock(UserActivityStoredHook.class);
		when(repository.insertIgnoreDuplicate(any(ActivityEvent.class))).thenReturn(true);
		UserActivityEventStoreService service = new UserActivityEventStoreService(repository, List.of(storedHook));

		// when
		service.store(sampleEvent());

		// then
		verify(storedHook).afterStored(any(ActivityEvent.class));
	}

	@Test
	@DisplayName("중복 이벤트면 저장 후 hook 실행을 건너뛴다")
	void store_skipsStoredHooksWhenDuplicate() {
		// given
		UserActivityEventJdbcRepository repository = mock(UserActivityEventJdbcRepository.class);
		UserActivityStoredHook storedHook = mock(UserActivityStoredHook.class);
		when(repository.insertIgnoreDuplicate(any(ActivityEvent.class))).thenReturn(false);
		UserActivityEventStoreService service = new UserActivityEventStoreService(repository, List.of(storedHook));

		// when
		service.store(sampleEvent());

		// then
		verify(storedHook, never()).afterStored(any(ActivityEvent.class));
	}

	@Test
	@DisplayName("저장 후 hook에서 예외가 발생해도 저장 흐름은 계속된다")
	void store_isolatesHookFailure() {
		// given
		UserActivityEventJdbcRepository repository = mock(UserActivityEventJdbcRepository.class);
		UserActivityStoredHook failedHook = mock(UserActivityStoredHook.class);
		when(repository.insertIgnoreDuplicate(any(ActivityEvent.class))).thenReturn(true);
		org.mockito.Mockito.doThrow(new IllegalStateException("hook failed"))
			.when(failedHook)
			.afterStored(any(ActivityEvent.class));
		UserActivityEventStoreService service = new UserActivityEventStoreService(repository, List.of(failedHook));

		// when & then
		assertThatCode(() -> service.store(sampleEvent())).doesNotThrowAnyException();
		verify(failedHook).afterStored(any(ActivityEvent.class));
	}

	private ActivityEvent sampleEvent() {
		return new ActivityEvent(
			"evt-1",
			"review.created",
			"v1",
			Instant.parse("2026-02-19T00:00:00Z"),
			10L,
			null,
			Map.of("restaurantId", 1L));
	}
}
