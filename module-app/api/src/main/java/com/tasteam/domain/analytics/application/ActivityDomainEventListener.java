package com.tasteam.domain.analytics.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tasteam.domain.group.event.GroupMemberJoinedEvent;
import com.tasteam.domain.review.event.ReviewCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 도메인 이벤트를 사용자 활동 수집 오케스트레이터로 연결하는 진입점입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityDomainEventListener {

	private final ActivityEventOrchestrator orchestrator;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onReviewCreated(ReviewCreatedEvent event) {
		handle(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onGroupMemberJoined(GroupMemberJoinedEvent event) {
		handle(event);
	}

	private void handle(Object event) {
		try {
			orchestrator.handleDomainEvent(event);
		} catch (Exception ex) {
			log.error("사용자 이벤트 수집 처리 중 예외가 발생해 작업을 건너뜁니다. eventType={}", event.getClass().getName(), ex);
		}
	}
}
