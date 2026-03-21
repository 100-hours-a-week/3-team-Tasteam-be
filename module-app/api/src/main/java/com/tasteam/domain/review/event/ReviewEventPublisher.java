package com.tasteam.domain.review.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ReviewEventPublisher {

	private final ApplicationEventPublisher publisher;

	public ReviewEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	public void publishReviewCreated(long restaurantId) {
		publishAfterCommit(new ReviewCreatedEvent(restaurantId));
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
