package com.tasteam.infra.messagequeue.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.MessageQueueMessage;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;

@UnitTest
@DisplayName("메시지큐 추적 서비스")
class MessageQueueTraceServiceTest {

	@Test
	@DisplayName("publish 추적을 저장한다")
	void recordPublish_savesTraceLog() {
		// given
		MessageQueueTraceLogRepository repository = mock(MessageQueueTraceLogRepository.class);
		when(repository.save(any(MessageQueueTraceLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
		MessageQueueTraceService service = new MessageQueueTraceService(repository, null);
		MessageQueueMessage message = MessageQueueMessage.of("domain.review.created", "123",
			"payload".getBytes(StandardCharsets.UTF_8));

		// when
		service.recordPublish(message, MessageQueueProviderType.REDIS_STREAM);

		// then
		ArgumentCaptor<MessageQueueTraceLog> captor = ArgumentCaptor.forClass(MessageQueueTraceLog.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getMessageId()).isEqualTo(message.messageId());
		assertThat(captor.getValue().getTopic()).isEqualTo(message.topic());
		assertThat(captor.getValue().getStage()).isEqualTo(MessageQueueTraceStage.PUBLISH);
	}

	@Test
	@DisplayName("consume 실패 추적을 저장한다")
	void recordConsumeFail_savesTraceLog() {
		// given
		MessageQueueTraceLogRepository repository = mock(MessageQueueTraceLogRepository.class);
		when(repository.save(any(MessageQueueTraceLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
		MessageQueueTraceService service = new MessageQueueTraceService(repository, null);
		MessageQueueMessage message = MessageQueueMessage.of("domain.group.member-joined", "200",
			"payload".getBytes(StandardCharsets.UTF_8));

		// when
		service.recordConsumeFail(
			message,
			MessageQueueProviderType.REDIS_STREAM,
			"tasteam-api",
			15L,
			new IllegalArgumentException("역직렬화 실패"));

		// then
		ArgumentCaptor<MessageQueueTraceLog> captor = ArgumentCaptor.forClass(MessageQueueTraceLog.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getStage()).isEqualTo(MessageQueueTraceStage.CONSUME_FAIL);
		assertThat(captor.getValue().getConsumerGroup()).isEqualTo("tasteam-api");
		assertThat(captor.getValue().getProcessingMillis()).isEqualTo(15L);
		assertThat(captor.getValue().getErrorMessage()).contains("역직렬화 실패");
	}

	@Test
	@DisplayName("messageId가 있으면 해당 이력만 조회한다")
	void findRecent_withMessageId_queriesByMessageId() {
		// given
		MessageQueueTraceLogRepository repository = mock(MessageQueueTraceLogRepository.class);
		Page<MessageQueueTraceLog> page = new PageImpl<>(List.of());
		when(repository.findAllByMessageIdOrderByIdDesc(eq("msg-1"), any(Pageable.class))).thenReturn(page);
		MessageQueueTraceService service = new MessageQueueTraceService(repository, null);

		// when
		service.findRecent("msg-1", 10);

		// then
		verify(repository).findAllByMessageIdOrderByIdDesc(eq("msg-1"), any(Pageable.class));
	}
}
