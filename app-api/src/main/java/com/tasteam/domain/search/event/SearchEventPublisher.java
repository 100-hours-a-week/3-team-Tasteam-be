package com.tasteam.domain.search.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SearchEventPublisher {

	private final ApplicationEventPublisher publisher;

	public void publish(SearchCompletedEvent event) {
		publisher.publishEvent(event);
	}
}
