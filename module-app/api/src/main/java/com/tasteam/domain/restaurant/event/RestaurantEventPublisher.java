package com.tasteam.domain.restaurant.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RestaurantEventPublisher {

	private final ApplicationEventPublisher publisher;

	public RestaurantEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	public void publishRestaurantCreated(long restaurantId) {
		publishAfterCommit(new RestaurantCreatedEvent(restaurantId));
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
