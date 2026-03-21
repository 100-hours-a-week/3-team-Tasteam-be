package com.tasteam.domain.analytics.ingest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "route", havingValue = "db")
public class ClientActivityDbSink implements ClientActivityIngestSink {

	private final UserActivityEventStoreService userActivityEventStoreService;

	@Override
	public String sinkType() {
		return "db";
	}

	@Override
	public void ingest(ActivityEvent event) {
		userActivityEventStoreService.store(event);
	}
}
