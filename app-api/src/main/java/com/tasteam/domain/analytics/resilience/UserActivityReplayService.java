package com.tasteam.domain.analytics.resilience;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;
import com.tasteam.infra.messagequeue.UserActivityMessageQueuePublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class UserActivityReplayService {

	private final UserActivitySourceOutboxService outboxService;
	private final UserActivityMessageQueuePublisher userActivityMessageQueuePublisher;
	private final MessageQueueProperties messageQueueProperties;
	private final ObjectMapper objectMapper;

	@Transactional
	public UserActivityReplayResult replayPending(int limit) {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			log.info("메시지큐 provider가 none이라 User Activity 재처리를 건너뜁니다.");
			return new UserActivityReplayResult(0, 0, 0);
		}

		List<UserActivitySourceOutboxEntry> candidates = outboxService.findReplayCandidates(limit);
		int successCount = 0;
		int failedCount = 0;
		for (UserActivitySourceOutboxEntry candidate : candidates) {
			ActivityEvent event = deserializeSafely(candidate);
			if (event == null) {
				failedCount++;
				continue;
			}

			try {
				userActivityMessageQueuePublisher.sink(event);
				successCount++;
			} catch (Exception ex) {
				failedCount++;
			}
		}
		return new UserActivityReplayResult(candidates.size(), successCount, failedCount);
	}

	private ActivityEvent deserializeSafely(UserActivitySourceOutboxEntry candidate) {
		try {
			return objectMapper.readValue(candidate.payloadJson(), ActivityEvent.class);
		} catch (Exception ex) {
			log.error("User Activity 재처리 payload 역직렬화에 실패했습니다. outboxId={}, eventId={}",
				candidate.id(), candidate.eventId(), ex);
			outboxService.markFailed(candidate.eventId(), ex);
			return null;
		}
	}
}
