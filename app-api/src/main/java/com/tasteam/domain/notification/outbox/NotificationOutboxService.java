package com.tasteam.domain.notification.outbox;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

	private final NotificationOutboxJdbcRepository outboxRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void enqueue(NotificationRequestedPayload payload) {
		String payloadJson = serializePayload(payload);
		outboxRepository.insertIfAbsent(
			payload.eventId(),
			payload.eventType(),
			payload.recipientId(),
			payloadJson);
	}

	@Transactional
	public void markPublished(String eventId) {
		outboxRepository.markPublished(eventId);
	}

	@Transactional
	public void markFailed(String eventId, String error) {
		outboxRepository.markFailed(eventId, error);
	}

	@Transactional(readOnly = true)
	public List<NotificationRequestedPayload> findCandidates(int limit) {
		return outboxRepository.findCandidates(limit).stream()
			.map(entry -> deserializePayload(entry.payloadJson()))
			.toList();
	}

	private String serializePayload(NotificationRequestedPayload payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (Exception ex) {
			throw new IllegalStateException("알림 아웃박스 payload 직렬화에 실패했습니다", ex);
		}
	}

	private NotificationRequestedPayload deserializePayload(String json) {
		try {
			return objectMapper.readValue(json, NotificationRequestedPayload.class);
		} catch (IOException ex) {
			throw new IllegalStateException("알림 아웃박스 payload 역직렬화에 실패했습니다", ex);
		}
	}
}
