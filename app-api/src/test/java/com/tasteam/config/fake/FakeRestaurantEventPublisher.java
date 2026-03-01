package com.tasteam.config.fake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.event.RestaurantEventPublisher;

@Component
@Primary
@Profile("test")
public class FakeRestaurantEventPublisher extends RestaurantEventPublisher {

	private final List<Long> publishedRestaurantIds = new ArrayList<>();

	public FakeRestaurantEventPublisher() {
		super(null);
	}

	@Override
	public void publishRestaurantCreated(long restaurantId) {
		publishedRestaurantIds.add(restaurantId);
	}

	public List<Long> getPublishedRestaurantIds() {
		return Collections.unmodifiableList(publishedRestaurantIds);
	}

	public void clear() {
		publishedRestaurantIds.clear();
	}
}
