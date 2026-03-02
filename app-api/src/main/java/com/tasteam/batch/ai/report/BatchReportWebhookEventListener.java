package com.tasteam.batch.ai.report;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tasteam.batch.ai.event.BatchExecutionFinishedEvent;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.infra.webhook.discord.BatchReportDiscordWebhookClient;
import com.tasteam.infra.webhook.discord.DiscordMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.webhook", name = "enabled", havingValue = "true")
public class BatchReportWebhookEventListener {

	private final AiJobRepository aiJobRepository;
	private final BatchReportDiscordMessageFactory messageFactory;
	private final BatchReportDiscordWebhookClient batchReportWebhookClient;

	@Async("webhookExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onBatchExecutionFinished(BatchExecutionFinishedEvent event) {
		try {
			var byTypeStatusCounts = aiJobRepository
				.countByBatchExecutionIdGroupByJobTypeAndStatus(event.batchExecutionId());
			DiscordMessage message = messageFactory.create(event, byTypeStatusCounts);
			batchReportWebhookClient.send(message);
		} catch (Exception e) {
			log.error("배치 리포트 웹훅 처리 실패. batchExecutionId={}", event.batchExecutionId(), e);
		}
	}
}
