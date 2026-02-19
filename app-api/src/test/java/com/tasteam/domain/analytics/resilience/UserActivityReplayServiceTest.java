package com.tasteam.domain.analytics.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;
import com.tasteam.infra.messagequeue.UserActivityMessageQueuePublisher;

@UnitTest
@DisplayName("사용자 이벤트 재처리 서비스")
class UserActivityReplayServiceTest {

	@Test
	@DisplayName("provider가 none이면 재처리를 수행하지 않는다")
	void replayPending_skipsWhenProviderNone() {
		// given
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivityMessageQueuePublisher publisher = mock(UserActivityMessageQueuePublisher.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider(MessageQueueProviderType.NONE.value());
		UserActivityReplayService replayService = new UserActivityReplayService(
			outboxService,
			publisher,
			properties,
			JsonMapper.builder().findAndAddModules().build());

		// when
		UserActivityReplayResult result = replayService.replayPending(100);

		// then
		assertThat(result.processedCount()).isZero();
		verifyNoInteractions(outboxService, publisher);
	}

	@Test
	@DisplayName("재처리 대상 payload를 역직렬화해 메시지 발행을 재시도한다")
	void replayPending_republishesCandidates() throws Exception {
		// given
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivityMessageQueuePublisher publisher = mock(UserActivityMessageQueuePublisher.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		UserActivityReplayService replayService = new UserActivityReplayService(
			outboxService,
			publisher,
			properties,
			objectMapper);

		ActivityEvent event = new ActivityEvent(
			"evt-1",
			"review.created",
			"v1",
			Instant.parse("2026-02-18T00:00:00Z"),
			10L,
			null,
			Map.of("restaurantId", 1L));
		UserActivitySourceOutboxEntry candidate = new UserActivitySourceOutboxEntry(
			1L,
			"evt-1",
			objectMapper.writeValueAsString(event),
			UserActivitySourceOutboxStatus.FAILED,
			2,
			Instant.now());
		when(outboxService.findReplayCandidates(100)).thenReturn(List.of(candidate));

		// when
		UserActivityReplayResult result = replayService.replayPending(100);

		// then
		assertThat(result.processedCount()).isEqualTo(1);
		assertThat(result.successCount()).isEqualTo(1);
		assertThat(result.failedCount()).isZero();
		ArgumentCaptor<ActivityEvent> eventCaptor = ArgumentCaptor.forClass(ActivityEvent.class);
		verify(publisher).sink(eventCaptor.capture());
		assertThat(eventCaptor.getValue().eventId()).isEqualTo("evt-1");
		assertThat(eventCaptor.getValue().eventName()).isEqualTo("review.created");
		assertThat(((Number)eventCaptor.getValue().properties().get("restaurantId")).longValue()).isEqualTo(1L);
	}

	@Test
	@DisplayName("재처리 payload가 손상되면 실패 상태를 갱신한다")
	void replayPending_marksFailedWhenPayloadInvalid() {
		// given
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivityMessageQueuePublisher publisher = mock(UserActivityMessageQueuePublisher.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		UserActivityReplayService replayService = new UserActivityReplayService(
			outboxService,
			publisher,
			properties,
			JsonMapper.builder().findAndAddModules().build());

		UserActivitySourceOutboxEntry candidate = new UserActivitySourceOutboxEntry(
			11L,
			"evt-broken",
			"{\"eventId\":\"missing-required-fields\"}",
			UserActivitySourceOutboxStatus.FAILED,
			3,
			Instant.now());
		when(outboxService.findReplayCandidates(100)).thenReturn(List.of(candidate));

		// when
		UserActivityReplayResult result = replayService.replayPending(100);

		// then
		assertThat(result.processedCount()).isEqualTo(1);
		assertThat(result.successCount()).isZero();
		assertThat(result.failedCount()).isEqualTo(1);
		verify(outboxService).markFailed(org.mockito.ArgumentMatchers.eq("evt-broken"),
			org.mockito.ArgumentMatchers.any());
		verifyNoInteractions(publisher);
	}
}
