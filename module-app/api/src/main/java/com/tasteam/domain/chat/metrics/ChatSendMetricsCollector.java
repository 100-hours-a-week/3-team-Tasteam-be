package com.tasteam.domain.chat.metrics;

import java.time.Duration;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class ChatSendMetricsCollector {

	@Nullable
	private final DistributionSummary sendDbQueriesPerRequest;
	@Nullable
	private final DistributionSummary sendDbSelectQueriesPerRequest;
	@Nullable
	private final DistributionSummary sendDbInsertQueriesPerRequest;
	@Nullable
	private final DistributionSummary sendTargetMemberCount;
	@Nullable
	private final DistributionSummary sendNotificationEventCount;
	@Nullable
	private final DistributionSummary sendProfileImageLookupCount;

	@Nullable
	private final Timer sendMessagePersistDuration;
	@Nullable
	private final Timer sendMemberFetchDuration;
	@Nullable
	private final Timer sendProfileImageFetchDuration;
	@Nullable
	private final Timer sendNotificationEventCreateDuration;
	@Nullable
	private final Timer sendTotalServiceDuration;
	@Nullable
	private final Counter chatSendFailureCounter;

	@Nullable
	private final Counter chatMemberCacheHitCounter;
	@Nullable
	private final Counter chatMemberCacheMissCounter;
	@Nullable
	private final Counter profileImageCacheHitCounter;
	@Nullable
	private final Counter profileImageCacheMissCounter;
	@Nullable
	private final Counter senderProfileCacheHitCounter;
	@Nullable
	private final Counter senderProfileCacheMissCounter;

	public ChatSendMetricsCollector(@Nullable
	MeterRegistry meterRegistry) {
		if (meterRegistry == null) {
			this.sendDbQueriesPerRequest = null;
			this.sendDbSelectQueriesPerRequest = null;
			this.sendDbInsertQueriesPerRequest = null;
			this.sendTargetMemberCount = null;
			this.sendNotificationEventCount = null;
			this.sendProfileImageLookupCount = null;
			this.sendMessagePersistDuration = null;
			this.sendMemberFetchDuration = null;
			this.sendProfileImageFetchDuration = null;
			this.sendNotificationEventCreateDuration = null;
			this.sendTotalServiceDuration = null;
			this.chatSendFailureCounter = null;
			this.chatMemberCacheHitCounter = null;
			this.chatMemberCacheMissCounter = null;
			this.profileImageCacheHitCounter = null;
			this.profileImageCacheMissCounter = null;
			this.senderProfileCacheHitCounter = null;
			this.senderProfileCacheMissCounter = null;
			return;
		}

		this.sendDbQueriesPerRequest = summary(meterRegistry, "chat_send_db_queries_per_request");
		this.sendDbSelectQueriesPerRequest = summary(meterRegistry, "chat_send_db_select_queries_per_request");
		this.sendDbInsertQueriesPerRequest = summary(meterRegistry, "chat_send_db_insert_queries_per_request");
		this.sendTargetMemberCount = summary(meterRegistry, "chat_send_target_member_count");
		this.sendNotificationEventCount = summary(meterRegistry, "chat_send_notification_event_count");
		this.sendProfileImageLookupCount = summary(meterRegistry, "chat_send_profile_image_lookup_count");

		this.sendMessagePersistDuration = timer(meterRegistry, "chat_send_message_persist_duration_seconds");
		this.sendMemberFetchDuration = timer(meterRegistry, "chat_send_member_fetch_duration_seconds");
		this.sendProfileImageFetchDuration = timer(meterRegistry, "chat_send_profile_image_fetch_duration_seconds");
		this.sendNotificationEventCreateDuration = timer(meterRegistry,
			"chat_send_notification_event_create_duration_seconds");
		this.sendTotalServiceDuration = timer(meterRegistry, "chat_send_total_service_duration_seconds");
		this.chatSendFailureCounter = meterRegistry.counter("chat_send_failure_total");

		this.chatMemberCacheHitCounter = meterRegistry.counter("chat_member_cache_hit_total");
		this.chatMemberCacheMissCounter = meterRegistry.counter("chat_member_cache_miss_total");
		this.profileImageCacheHitCounter = meterRegistry.counter("profile_image_cache_hit_total");
		this.profileImageCacheMissCounter = meterRegistry.counter("profile_image_cache_miss_total");
		this.senderProfileCacheHitCounter = meterRegistry.counter("sender_profile_cache_hit_total");
		this.senderProfileCacheMissCounter = meterRegistry.counter("sender_profile_cache_miss_total");
	}

	public void recordDbQueryCount(long total, long select, long insert) {
		record(sendDbQueriesPerRequest, total);
		record(sendDbSelectQueriesPerRequest, select);
		record(sendDbInsertQueriesPerRequest, insert);
	}

	public void recordTargetMemberCount(int count) {
		record(sendTargetMemberCount, count);
	}

	public void recordNotificationEventCount(int count) {
		record(sendNotificationEventCount, count);
	}

	public void recordProfileImageLookupCount(int count) {
		record(sendProfileImageLookupCount, count);
	}

	public void recordMessagePersistDuration(Duration duration) {
		record(sendMessagePersistDuration, duration);
	}

	public void recordMemberFetchDuration(Duration duration) {
		record(sendMemberFetchDuration, duration);
	}

	public void recordProfileImageFetchDuration(Duration duration) {
		record(sendProfileImageFetchDuration, duration);
	}

	public void recordNotificationEventCreateDuration(Duration duration) {
		record(sendNotificationEventCreateDuration, duration);
	}

	public void recordTotalServiceDuration(Duration duration) {
		record(sendTotalServiceDuration, duration);
	}

	public void recordSendFailure() {
		recordCounter(chatSendFailureCounter);
	}

	public void recordChatMemberCacheHit() {
		recordCounter(chatMemberCacheHitCounter);
	}

	public void recordChatMemberCacheMiss() {
		recordCounter(chatMemberCacheMissCounter);
	}

	public void recordProfileImageCacheHit() {
		recordCounter(profileImageCacheHitCounter);
	}

	public void recordProfileImageCacheMiss() {
		recordCounter(profileImageCacheMissCounter);
	}

	public void recordSenderProfileCacheHit() {
		recordCounter(senderProfileCacheHitCounter);
	}

	public void recordSenderProfileCacheMiss() {
		recordCounter(senderProfileCacheMissCounter);
	}

	private DistributionSummary summary(MeterRegistry meterRegistry, String metricName) {
		return DistributionSummary.builder(metricName)
			.publishPercentileHistogram()
			.register(meterRegistry);
	}

	private Timer timer(MeterRegistry meterRegistry, String metricName) {
		return Timer.builder(metricName)
			.publishPercentileHistogram()
			.register(meterRegistry);
	}

	private void record(@Nullable
	DistributionSummary summary, long value) {
		if (summary == null || value < 0) {
			return;
		}
		summary.record(value);
	}

	private void record(@Nullable
	Timer timer, Duration duration) {
		if (timer == null || duration.isNegative()) {
			return;
		}
		timer.record(duration);
	}

	private void recordCounter(@Nullable
	Counter counter) {
		if (counter != null) {
			counter.increment();
		}
	}
}
