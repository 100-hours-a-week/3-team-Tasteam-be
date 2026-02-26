package com.tasteam.infra.messagequeue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;

@UnitTest
@DisplayName("사용자 이벤트 source outbox sink")
class UserActivitySourceOutboxSinkTest {

	@Test
	@DisplayName("이벤트를 수신하면 source outbox에 적재한다")
	void sink_enqueuesEvent() {
		// given
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivitySourceOutboxSink sink = new UserActivitySourceOutboxSink(outboxService);
		ActivityEvent event = new ActivityEvent(
			"evt-1",
			"group.joined",
			"v1",
			Instant.parse("2026-02-18T00:00:00Z"),
			1L,
			null,
			Map.of("groupId", 33L));

		// when
		sink.sink(event);

		// then
		verify(outboxService).enqueue(event);
	}
}
