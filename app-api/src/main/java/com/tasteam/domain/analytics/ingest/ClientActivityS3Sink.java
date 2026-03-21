package com.tasteam.domain.analytics.ingest;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.infra.messagequeue.UserActivityS3SinkPublisher;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientActivityS3Sink implements ClientActivityIngestSink {

	private final UserActivityS3SinkPublisher userActivityS3SinkPublisher;

	@Override
	public String sinkType() {
		return "s3";
	}

	@Override
	public void ingest(ActivityEvent event) {
		userActivityS3SinkPublisher.sink(event);
	}
}
