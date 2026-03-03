package com.tasteam.domain.subgroup.event;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SubgroupEventPublisher {

	private final ApplicationEventPublisher publisher;

	public SubgroupEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	public void publishMemberJoined(Long groupId, Long subgroupId, Long memberId, String subgroupName,
		Instant joinedAt) {
		publishAfterCommit(new SubgroupMemberJoinedEvent(groupId, subgroupId, memberId, subgroupName, joinedAt));
	}

	private void publishAfterCommit(Object event) {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					publisher.publishEvent(event);
				}
			});
			return;
		}
		publisher.publishEvent(event);
	}
}
