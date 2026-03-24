package com.tasteam.domain.main.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.event.RestaurantChangedEvent;
import com.tasteam.domain.restaurant.event.RestaurantCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MainCacheInvalidationEventListener {

	private final MainCacheInvalidationService mainCacheInvalidationService;

	@EventListener
	public void onRestaurantCreated(RestaurantCreatedEvent event) {
		mainCacheInvalidationService.evictHomeCaches(event.restaurantId());
	}

	@EventListener
	public void onRestaurantChanged(RestaurantChangedEvent event) {
		mainCacheInvalidationService.evictHomeCaches(event.restaurantId());
	}
}
