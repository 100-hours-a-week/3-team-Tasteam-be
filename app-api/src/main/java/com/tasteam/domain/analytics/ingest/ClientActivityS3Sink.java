package com.tasteam.domain.analytics.ingest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.infra.messagequeue.UserActivityS3SinkPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "route", havingValue = "s3", matchIfMissing = true)
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
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
