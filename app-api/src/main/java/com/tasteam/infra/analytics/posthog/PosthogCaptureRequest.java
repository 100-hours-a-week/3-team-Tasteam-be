package com.tasteam.infra.analytics.posthog;

import java.time.Instant;
import java.util.Map;

record PosthogCaptureRequest(
	String api_key,
	String event,
	String distinct_id,
	Instant timestamp,
	Map<String, Object> properties) {
}
