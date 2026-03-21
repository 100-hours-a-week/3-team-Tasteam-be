package com.tasteam.domain.notification.outbox;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;

@UnitTest
@DisplayName("[유닛](Notification) NotificationOutboxService 단위 테스트")
class NotificationOutboxServiceTest {

	@Mock
	private NotificationOutboxJdbcRepository outboxRepository;

	@InjectMocks
	private NotificationOutboxService outboxService;

	// @InjectMocks는 objectMapper를 주입하지 못하므로 직접 주입
	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	@Nested
	@DisplayName("enqueue()")
	class Enqueue {

		@Test
		@DisplayName("올바른 필드로 insertIfAbsent를 호출한다")
		void enqueue_insertsToRepository() throws Exception {
			// given
			NotificationOutboxService service = new NotificationOutboxService(outboxRepository, objectMapper);
			NotificationRequestedPayload payload = samplePayload("evt-001");
			doReturn(true).when(outboxRepository).insertIfAbsent(anyString(), anyString(), eq(10L), anyString());

			// when
			// MANDATORY는 Spring proxy에서만 동작하므로 직접 내부 로직 검증
			// — MANDATORY 위반 검증은 NotificationOutboxAtomicityIntegrationTest 담당
			assertThatCode(() -> service.enqueue(payload)).doesNotThrowAnyException();

			// then: eventId, eventType, recipientId가 올바르게 전달됐는지 확인
			then(outboxRepository).should().insertIfAbsent(
				eq("evt-001"),
				eq("GroupMemberJoinedEvent"),
				eq(10L),
				anyString());
		}

		@Test
		@DisplayName("payload 직렬화 실패 시 IllegalStateException을 던진다")
		void enqueue_serializationFailure_propagates() throws Exception {
			// given
			ObjectMapper brokenMapper = new ObjectMapper() {
				@Override
				public String writeValueAsString(Object value)
					throws com.fasterxml.jackson.core.JsonProcessingException {
					throw new com.fasterxml.jackson.core.JsonProcessingException("broken") {};
				}
			};
			NotificationOutboxService service = new NotificationOutboxService(outboxRepository, brokenMapper);
			NotificationRequestedPayload payload = samplePayload("evt-broken");

			// when / then
			assertThatThrownBy(() -> service.enqueue(payload))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("payload 직렬화");
		}
	}

	@Nested
	@DisplayName("markPublished()")
	class MarkPublished {

		@Test
		@DisplayName("eventId를 repository에 위임한다")
		void markPublished_delegatesToRepository() {
			NotificationOutboxService service = new NotificationOutboxService(outboxRepository, objectMapper);

			service.markPublished("evt-001");

			then(outboxRepository).should().markPublished("evt-001");
		}
	}

	@Nested
	@DisplayName("markFailed()")
	class MarkFailed {

		@Test
		@DisplayName("eventId와 에러 메시지를 repository에 위임한다")
		void markFailed_delegatesToRepository() {
			NotificationOutboxService service = new NotificationOutboxService(outboxRepository, objectMapper);

			service.markFailed("evt-001", "publish timeout");

			then(outboxRepository).should().markFailed("evt-001", "publish timeout");
		}
	}

	private NotificationRequestedPayload samplePayload(String eventId) {
		return new NotificationRequestedPayload(
			eventId,
			"GroupMemberJoinedEvent",
			10L,
			NotificationType.SYSTEM,
			List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
			"group-joined",
			Map.of("title", "그룹 가입 완료", "body", "테스트 그룹에 가입되었습니다."),
			"/groups/1",
			Instant.parse("2026-03-15T00:00:00Z"));
	}
}
