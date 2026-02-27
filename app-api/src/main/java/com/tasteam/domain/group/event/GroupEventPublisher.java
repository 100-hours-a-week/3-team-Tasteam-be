package com.tasteam.domain.group.event;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class GroupEventPublisher {

	private final ApplicationEventPublisher publisher;

	public GroupEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	public void publishMemberJoined(Long groupId, Long memberId, String groupName, Instant joinedAt) {
		publishAfterCommit(new GroupMemberJoinedEvent(groupId, memberId, groupName, joinedAt));
	}

	public void publishRequestSubmitted(Long groupId, Long applicantMemberId, Long ownerId, String groupName,
		Instant submittedAt) {
		publishAfterCommit(new GroupRequestSubmittedEvent(groupId, applicantMemberId, ownerId, groupName, submittedAt));
	}

	public void publishRequestReviewed(Long groupId, Long applicantMemberId, String groupName,
		GroupRequestReviewedEvent.ReviewResult result, String reason, Instant reviewedAt) {
		publishAfterCommit(
			new GroupRequestReviewedEvent(groupId, applicantMemberId, groupName, result, reason, reviewedAt));
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
