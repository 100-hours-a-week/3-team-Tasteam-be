package com.tasteam.domain.analytics.ingest;

import com.tasteam.domain.analytics.api.ActivityEvent;

public interface ClientActivityIngestSink {

	String sinkType();

	void ingest(ActivityEvent event);
}
