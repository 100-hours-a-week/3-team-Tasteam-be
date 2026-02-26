package com.tasteam.batch.ai.report;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.batch.ai.event.BatchExecutionFinishedEvent;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.infra.webhook.discord.BatchReportDiscordWebhookClient;
import com.tasteam.infra.webhook.discord.DiscordMessage;

@UnitTest
@DisplayName("BatchReportWebhookEventListener")
class BatchReportWebhookEventListenerTest {

	@Mock
	private AiJobRepository aiJobRepository;

	@Mock
	private BatchReportDiscordMessageFactory messageFactory;

	@Mock
	private BatchReportDiscordWebhookClient batchReportWebhookClient;

	@InjectMocks
	private BatchReportWebhookEventListener listener;

	@Test
	@DisplayName("배치 완료 이벤트 수신 시 리포트 웹훅을 전송한다")
	void onBatchExecutionFinished_sendsReportWebhook() {
		BatchExecutionFinishedEvent event = new BatchExecutionFinishedEvent(
			1L, BatchType.REVIEW_ANALYSIS_DAILY, BatchExecutionStatus.COMPLETED,
			Instant.now(), Instant.now(), 10, 8, 2, 0);
		given(aiJobRepository.countByBatchExecutionIdGroupByJobTypeAndStatus(1L)).willReturn(List.of());
		DiscordMessage message = new DiscordMessage(List.of());
		given(messageFactory.create(event, List.of())).willReturn(message);

		listener.onBatchExecutionFinished(event);

		then(batchReportWebhookClient).should().send(message);
	}

	@Test
	@DisplayName("처리 중 예외가 발생해도 전파되지 않는다")
	void onBatchExecutionFinished_whenExceptionOccurs_doesNotPropagate() {
		BatchExecutionFinishedEvent event = new BatchExecutionFinishedEvent(
			1L, BatchType.REVIEW_ANALYSIS_DAILY, BatchExecutionStatus.FAILED,
			Instant.now(), Instant.now(), 10, 0, 10, 0);
		given(aiJobRepository.countByBatchExecutionIdGroupByJobTypeAndStatus(1L))
			.willThrow(new RuntimeException("DB 오류"));

		assertThatCode(() -> listener.onBatchExecutionFinished(event))
			.doesNotThrowAnyException();
	}
}
