package com.tasteam.domain.analytics.ingest;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
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
