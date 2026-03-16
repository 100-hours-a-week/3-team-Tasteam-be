package com.tasteam.domain.chat.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.domain.chat.config.ChatStreamProperties;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.repository.ChatRoomRepository;
import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ChatStreamSubscriber {
	private static final Duration POLL_MIN_IDLE = Duration.ofMinutes(1);
	private static final int PENDING_SCAN_COUNT = 100;
	private static final int MAX_RETRY_COUNT = 3;
	private static final int MAX_STREAM_LENGTH = 1000;
	private static final String PARTITION_STREAM_PREFIX = "chat:partition:";

	private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
	private final StringRedisTemplate stringRedisTemplate;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatStreamKeyResolver keyResolver;
	private final ChatStreamGroupNameProvider groupNameProvider;
	private final SimpMessagingTemplate messagingTemplate;
	private final ChatStreamProperties chatStreamProperties;
	private final ChatWsBroadcastPublisher chatWsBroadcastPublisher;
	@Nullable
	private final MeterRegistry meterRegistry;

	private final Map<Integer, Subscription> partitionSubscriptions = new ConcurrentHashMap<>();
	private final Map<Long, Subscription> legacyRoomSubscriptions = new ConcurrentHashMap<>();
	private final Map<Integer, AtomicLong> partitionPendingMessages = new ConcurrentHashMap<>();

	@Autowired
	public ChatStreamSubscriber(
		@Qualifier("chatStreamListenerContainer")
		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
		StringRedisTemplate stringRedisTemplate,
		ChatRoomRepository chatRoomRepository,
		ChatStreamKeyResolver keyResolver,
		ChatStreamGroupNameProvider groupNameProvider,
		SimpMessagingTemplate messagingTemplate,
		ChatStreamProperties chatStreamProperties,
		ChatWsBroadcastPublisher chatWsBroadcastPublisher,
		@Nullable
		MeterRegistry meterRegistry) {
		this.container = container;
		this.stringRedisTemplate = stringRedisTemplate;
		this.chatRoomRepository = chatRoomRepository;
		this.keyResolver = keyResolver;
		this.groupNameProvider = groupNameProvider;
		this.messagingTemplate = messagingTemplate;
		this.chatStreamProperties = chatStreamProperties;
		this.chatWsBroadcastPublisher = chatWsBroadcastPublisher;
		this.meterRegistry = meterRegistry;
	}

	@PostConstruct
	public void start() {
		if (!chatStreamProperties.enabled()) {
			log.info("채팅 스트림 구독 기능이 비활성화되어 시작을 건너뜁니다.");
			return;
		}
		container.start();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		if (!chatStreamProperties.enabled()) {
			log.info("채팅 스트림 구독 부트스트랩을 건너뜁니다. enabled={}", chatStreamProperties.enabled());
			return;
		}

		validateConfiguration();
		logConfiguration();

		if (chatStreamProperties.partitionConsumeEnabled()) {
			ensurePartitionGroupsAtStartup();
			ensurePartitionSubscriptions();
		}

		if (chatStreamProperties.legacyRoomConsumeEnabled() && chatStreamProperties.bootstrapEnabled()) {
			refreshLegacyRoomSubscriptions();
		}
	}

	@PreDestroy
	public void stop() {
		container.stop();
	}

	@Scheduled(fixedDelayString = "PT30S")
	public void refreshLegacyRoomSubscriptions() {
		if (!chatStreamProperties.enabled() || !chatStreamProperties.legacyRoomConsumeEnabled()) {
			return;
		}
		if (chatStreamProperties.maxTotalSubscriptions() <= 0 || chatStreamProperties.bootstrapBatchSize() <= 0) {
			return;
		}

		Set<Long> currentRoomIds = Set.copyOf(chatRoomRepository.findActiveRoomIds());
		int remainingCapacity = Math.min(
			chatStreamProperties.bootstrapBatchSize(),
			Math.max(0, chatStreamProperties.maxTotalSubscriptions() - legacyRoomSubscriptions.size()));
		if (remainingCapacity <= 0) {
			return;
		}

		int subscribedThisRound = 0;
		for (Long roomId : currentRoomIds) {
			if (subscribedThisRound >= remainingCapacity) {
				break;
			}
			if (legacyRoomSubscriptions.containsKey(roomId) || roomId == null) {
				continue;
			}
			try {
				legacyRoomSubscriptions.computeIfAbsent(roomId, this::registerLegacyRoomSubscription);
				subscribedThisRound++;
			} catch (Exception ex) {
				log.warn("레거시 채팅방 구독 등록 실패, 다음 주기에 재시도합니다. roomId={}", roomId, ex);
			}
		}

		if (subscribedThisRound > 0) {
			log.info("레거시 채팅방 구독 {}건 등록 (현재 레거시 구독 수: {})", subscribedThisRound, legacyRoomSubscriptions.size());
		}
	}

	@Scheduled(fixedDelayString = "PT30S")
	public void retryPartitionSubscriptions() {
		if (!chatStreamProperties.enabled() || !chatStreamProperties.partitionConsumeEnabled()) {
			return;
		}
		if (partitionSubscriptions.size() >= chatStreamProperties.partitionCount()) {
			return;
		}
		ensurePartitionSubscriptions();
	}

	public void ensureSubscribed(Long roomId) {
		if (roomId == null || !chatStreamProperties.enabled() || !chatStreamProperties.legacyRoomConsumeEnabled()) {
			return;
		}
		legacyRoomSubscriptions.computeIfAbsent(roomId, this::registerLegacyRoomSubscription);
	}

	@Scheduled(fixedDelayString = "PT30S")
	public void recoverPendingMessages() {
		if (!chatStreamProperties.enabled()) {
			return;
		}

		StreamOperations<String, String, String> streamOperations = stringRedisTemplate.opsForStream();
		if (chatStreamProperties.partitionConsumeEnabled()) {
			for (int partitionId = 0; partitionId < chatStreamProperties.partitionCount(); partitionId++) {
				String streamKey = keyResolver.partitionStreamKey(partitionId);
				recoverPendingMessages(streamOperations, streamKey, partitionId);
			}
		}
		if (chatStreamProperties.legacyRoomConsumeEnabled()) {
			for (Long roomId : legacyRoomSubscriptions.keySet()) {
				String streamKey = keyResolver.roomStreamKey(roomId);
				recoverPendingMessages(streamOperations, streamKey, null);
			}
		}
	}

	private void validateConfiguration() {
		if (chatStreamProperties.partitionCount() <= 0) {
			throw new IllegalStateException("chat.stream.partition-count must be greater than 0");
		}
		if (chatStreamProperties.maxAllowedPartitions() <= 0) {
			throw new IllegalStateException("chat.stream.max-allowed-partitions must be greater than 0");
		}
		if (chatStreamProperties.partitionCount() > chatStreamProperties.maxAllowedPartitions()) {
			throw new IllegalStateException(
				"chat.stream.partition-count must be <= chat.stream.max-allowed-partitions");
		}
		if (chatStreamProperties.executorThreadPoolSize() <= 0) {
			throw new IllegalStateException("chat.stream.executor-thread-pool-size must be greater than 0");
		}
		if (chatStreamProperties.executorQueueCapacity() < 0) {
			throw new IllegalStateException("chat.stream.executor-queue-capacity must be >= 0");
		}
	}

	private void logConfiguration() {
		log.info(
			"Chat stream config: partitionCount={}, executorThreads={}, partitionConsumeEnabled={}, legacyRoomConsumeEnabled={}, dualWriteEnabled={}, wsPubSubBroadcastEnabled={}, wsPubSubChannel={}",
			chatStreamProperties.partitionCount(),
			chatStreamProperties.executorThreadPoolSize(),
			chatStreamProperties.partitionConsumeEnabled(),
			chatStreamProperties.legacyRoomConsumeEnabled(),
			chatStreamProperties.dualWriteEnabled(),
			chatStreamProperties.wsPubSubBroadcastEnabled(),
			chatStreamProperties.wsPubSubChannel());

		double ratio = (double)chatStreamProperties.partitionCount() / chatStreamProperties.executorThreadPoolSize();
		if (chatStreamProperties.partitionConsumeEnabled() && ratio > 8.0d) {
			log.warn(
				"Partition to executor ratio may be too high. partitionCount={}, executorThreads={}, ratio={}",
				chatStreamProperties.partitionCount(),
				chatStreamProperties.executorThreadPoolSize(),
				String.format("%.2f", ratio));
		}
	}

	private void ensurePartitionGroupsAtStartup() {
		int createdCount = 0;
		int alreadyExistsCount = 0;
		int failedCount = 0;
		List<Integer> failedPartitions = new ArrayList<>();

		for (int partitionId = 0; partitionId < chatStreamProperties.partitionCount(); partitionId++) {
			String streamKey = keyResolver.partitionStreamKey(partitionId);
			try {
				EnsureGroupResult result = ensureGroupExists(streamKey);
				if (result == EnsureGroupResult.CREATED) {
					createdCount++;
				} else {
					alreadyExistsCount++;
				}
				registerPartitionPendingGauge(partitionId, streamKey);
			} catch (Exception ex) {
				failedCount++;
				failedPartitions.add(partitionId);
				log.error("파티션 consumer group 초기화 실패. partitionId={}, streamKey={}", partitionId, streamKey, ex);
			}
		}

		log.info(
			"Partition consumer group ensure completed. created={}, alreadyExists={}, failed={}",
			createdCount,
			alreadyExistsCount,
			failedCount);
		if (failedCount > 0) {
			log.warn("Partition consumer group ensure failed partitions={}", failedPartitions);
		}
	}

	private void ensurePartitionSubscriptions() {
		int subscribedThisRound = 0;
		for (int partitionId = 0; partitionId < chatStreamProperties.partitionCount(); partitionId++) {
			if (partitionSubscriptions.containsKey(partitionId)) {
				continue;
			}
			final int currentPartitionId = partitionId;
			try {
				partitionSubscriptions.computeIfAbsent(currentPartitionId, this::registerPartitionSubscription);
				subscribedThisRound++;
			} catch (Exception ex) {
				log.warn("파티션 구독 등록 실패, 다음 주기에 재시도합니다. partitionId={}", currentPartitionId, ex);
			}
		}

		if (subscribedThisRound > 0) {
			log.info("파티션 구독 {}건 등록 (현재 파티션 구독 수: {})", subscribedThisRound, partitionSubscriptions.size());
		}
	}

	private Subscription registerPartitionSubscription(int partitionId) {
		String streamKey = keyResolver.partitionStreamKey(partitionId);
		ensureGroupExists(streamKey);
		Consumer consumer = Consumer.from(groupNameProvider.groupName(), groupNameProvider.consumerName());
		return container.receive(consumer,
			StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
			record -> handleRecord(record, partitionId, streamKey));
	}

	private Subscription registerLegacyRoomSubscription(Long roomId) {
		String streamKey = keyResolver.roomStreamKey(roomId);
		// Legacy room consume 경로는 room stream이 런타임에 생성될 수 있어 호환을 위해 유지한다.
		ensureGroupExists(streamKey);
		Consumer consumer = Consumer.from(groupNameProvider.groupName(), groupNameProvider.consumerName());
		return container.receive(consumer,
			StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
			record -> handleRecord(record, null, streamKey));
	}

	private void recoverPendingMessages(
		StreamOperations<String, String, String> streamOperations,
		String streamKey,
		@Nullable
		Integer partitionId) {
		try {
			PendingMessagesSummary pendingSummary = streamOperations.pending(streamKey, groupNameProvider.groupName());
			long totalPending = pendingSummary == null ? 0L : pendingSummary.getTotalPendingMessages();
			updatePendingBacklog(partitionId, streamKey, totalPending);
			if (totalPending <= 0) {
				return;
			}

			PendingMessages pending = streamOperations.pending(
				streamKey,
				groupNameProvider.groupName(),
				Range.unbounded(),
				PENDING_SCAN_COUNT);
			if (pending == null || pending.isEmpty()) {
				return;
			}

			List<RecordId> toRetry = new ArrayList<>();
			List<RecordId> toDeadLetter = new ArrayList<>();
			for (PendingMessage message : pending) {
				if (message.getElapsedTimeSinceLastDelivery().compareTo(POLL_MIN_IDLE) < 0) {
					continue;
				}
				if (message.getTotalDeliveryCount() >= MAX_RETRY_COUNT) {
					toDeadLetter.add(message.getId());
				} else {
					toRetry.add(message.getId());
				}
			}

			claimAndHandle(streamKey, toRetry, false, partitionId);
			claimAndHandle(streamKey, toDeadLetter, true, partitionId);
		} catch (DataAccessException ex) {
			if (isNoGroupError(ex)) {
				handleNoGroup(partitionId, streamKey, "pending_recovery");
				return;
			}
			log.warn(
				"Pending recovery failed. partitionId={}, streamKey={}, errorType={}",
				partitionLabel(partitionId),
				streamKey,
				"redis_recovery_error",
				ex);
			recordErrorMetric(partitionLabel(partitionId), streamKey, "redis_recovery_error");
		}
	}

	private void handleRecord(MapRecord<String, String, String> record, @Nullable
	Integer partitionId, String streamKey) {
		long startNanos = System.nanoTime();
		Integer resolvedPartitionId = partitionId == null ? resolvePartitionId(record.getStream()) : partitionId;
		String partitionLabel = partitionLabel(resolvedPartitionId);

		try {
			ChatStreamPayload payload = ChatStreamPayload.fromMap(record.getValue());
			if (!broadcastToWebSocket(payload, partitionLabel, streamKey, record.getId())) {
				return;
			}
			if (ack(record)) {
				recordSuccessMetrics(partitionLabel, streamKey, Duration.ofNanos(System.nanoTime() - startNanos));
			}
		} catch (TaskRejectedException ex) {
			log.warn(
				"Failed to process chat stream record. partitionId={}, streamKey={}, errorType={}, recordId={}",
				partitionLabel,
				streamKey,
				"task_rejected",
				record.getId(),
				ex);
			recordErrorMetric(partitionLabel, streamKey, "task_rejected");
		} catch (DataAccessException ex) {
			log.warn(
				"Failed to process chat stream record. partitionId={}, streamKey={}, errorType={}, recordId={}",
				partitionLabel,
				streamKey,
				"redis_error",
				record.getId(),
				ex);
			recordErrorMetric(partitionLabel, streamKey, "redis_error");
		} catch (Exception ex) {
			log.warn(
				"Failed to process chat stream record. partitionId={}, streamKey={}, errorType={}, recordId={}",
				partitionLabel,
				streamKey,
				"processing_error",
				record.getId(),
				ex);
			recordErrorMetric(partitionLabel, streamKey, "processing_error");
		}
	}

	private boolean broadcastToWebSocket(
		ChatStreamPayload payload,
		String partitionLabel,
		String streamKey,
		RecordId recordId) {
		if (chatStreamProperties.wsPubSubBroadcastEnabled()) {
			boolean published = chatWsBroadcastPublisher.publish(payload);
			if (!published) {
				log.warn(
					"Failed to process chat stream record. partitionId={}, streamKey={}, errorType={}, recordId={}",
					partitionLabel,
					streamKey,
					"ws_pubsub_publish_failed",
					recordId);
				recordErrorMetric(partitionLabel, streamKey, "ws_pubsub_publish_failed");
				return false;
			}
			return true;
		}

		ChatMessageItemResponse itemResponse = payload.toMessageItem();
		messagingTemplate.convertAndSend("/topic/chat-rooms/" + payload.chatRoomId(), itemResponse);
		return true;
	}

	private void claimAndHandle(String streamKey, List<RecordId> recordIds, boolean deadLetter, @Nullable
	Integer partitionId) {
		if (recordIds.isEmpty()) {
			return;
		}

		StreamOperations<String, String, String> streamOperations = stringRedisTemplate.opsForStream();
		XClaimOptions claimOptions = XClaimOptions.minIdle(POLL_MIN_IDLE).ids(recordIds.toArray(RecordId[]::new));
		List<MapRecord<String, String, String>> claimed;
		try {
			claimed = streamOperations.claim(
				streamKey,
				groupNameProvider.groupName(),
				groupNameProvider.consumerName(),
				claimOptions);
		} catch (DataAccessException ex) {
			if (isNoGroupError(ex)) {
				handleNoGroup(partitionId, streamKey, "pending_claim");
				return;
			}
			log.warn(
				"Pending claim failed. partitionId={}, streamKey={}, errorType={}, deadLetter={}",
				partitionLabel(partitionId),
				streamKey,
				"redis_claim_error",
				deadLetter,
				ex);
			recordErrorMetric(partitionLabel(partitionId), streamKey, "redis_claim_error");
			return;
		}

		if (claimed == null || claimed.isEmpty()) {
			return;
		}
		for (MapRecord<String, String, String> record : claimed) {
			if (deadLetter) {
				boolean deadLettered = sendToDeadLetter(record, partitionId, streamKey);
				if (deadLettered) {
					ack(record);
				}
			} else {
				handleRecord(record, partitionId, streamKey);
			}
		}
	}

	private boolean sendToDeadLetter(MapRecord<String, String, String> record, @Nullable
	Integer partitionId, String streamKey) {
		String deadLetterKey = keyResolver.deadLetterStreamKey();
		Map<String, String> value = new HashMap<>(record.getValue());
		value.put("sourceStreamKey", streamKey);
		value.put("sourceRecordId", record.getId().getValue());
		if (partitionId != null) {
			value.put("partitionId", String.valueOf(partitionId));
		}

		MapRecord<String, String, String> dlqRecord = StreamRecords.newRecord()
			.in(deadLetterKey)
			.ofMap(value);
		try {
			stringRedisTemplate.opsForStream().add(
				dlqRecord,
				org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions
					.maxlen(MAX_STREAM_LENGTH)
					.approximateTrimming(true));
			return true;
		} catch (DataAccessException ex) {
			log.warn(
				"DLQ publish failed. partitionId={}, streamKey={}, errorType={}, recordId={}",
				partitionLabel(partitionId),
				streamKey,
				"redis_dlq_error",
				record.getId(),
				ex);
			recordErrorMetric(partitionLabel(partitionId), streamKey, "redis_dlq_error");
			return false;
		}
	}

	private boolean ack(MapRecord<String, String, String> record) {
		try {
			stringRedisTemplate.opsForStream().acknowledge(
				record.getStream(),
				groupNameProvider.groupName(),
				record.getId());
			return true;
		} catch (DataAccessException ex) {
			String streamKey = record.getStream();
			Integer partitionId = resolvePartitionId(streamKey);
			if (isNoGroupError(ex)) {
				handleNoGroup(partitionId, streamKey, "ack");
				return false;
			}
			log.warn(
				"ACK failed. partitionId={}, streamKey={}, errorType={}, recordId={}",
				partitionLabel(partitionId),
				streamKey,
				"redis_ack_error",
				record.getId(),
				ex);
			recordErrorMetric(partitionLabel(partitionId), streamKey, "redis_ack_error");
			return false;
		}
	}

	private EnsureGroupResult ensureGroupExists(String streamKey) {
		return ensureGroupExists(streamKey, false);
	}

	private EnsureGroupResult ensureGroupExists(String streamKey, boolean suppressLog) {
		try {
			stringRedisTemplate.execute((RedisCallback<Void>)connection -> {
				connection.streamCommands().xGroupCreate(
					stringRedisTemplate.getStringSerializer().serialize(streamKey),
					groupNameProvider.groupName(),
					ReadOffset.latest(),
					true);
				return null;
			});
			if (!suppressLog) {
				log.debug("Created consumer group. streamKey={}, groupName={}", streamKey,
					groupNameProvider.groupName());
			}
			return EnsureGroupResult.CREATED;
		} catch (DataAccessException ex) {
			if (isBusyGroupError(ex)) {
				return EnsureGroupResult.ALREADY_EXISTS;
			}
			throw ex;
		} catch (RuntimeException ex) {
			if (isBusyGroupError(ex)) {
				return EnsureGroupResult.ALREADY_EXISTS;
			}
			throw ex;
		}
	}

	private void handleNoGroup(@Nullable
	Integer partitionId, String streamKey, String phase) {
		log.warn(
			"NOGROUP detected. partitionId={}, streamKey={}, phase={}. consumer group를 재생성합니다.",
			partitionLabel(partitionId),
			streamKey,
			phase);
		try {
			ensureGroupExists(streamKey, true);
			if (partitionId != null) {
				resubscribePartition(partitionId);
			}
			log.info(
				"NOGROUP recovery completed. partitionId={}, streamKey={}, phase={}",
				partitionLabel(partitionId),
				streamKey,
				phase);
		} catch (Exception recoveryEx) {
			log.error(
				"NOGROUP recovery failed. partitionId={}, streamKey={}, phase={}",
				partitionLabel(partitionId),
				streamKey,
				phase,
				recoveryEx);
		}
	}

	private void resubscribePartition(int partitionId) {
		Subscription existing = partitionSubscriptions.remove(partitionId);
		if (existing != null) {
			try {
				existing.cancel();
			} catch (Exception cancelEx) {
				log.debug("기존 파티션 구독 cancel 중 예외를 무시합니다. partitionId={}", partitionId, cancelEx);
			}
		}
		try {
			partitionSubscriptions.put(partitionId, registerPartitionSubscription(partitionId));
			log.info("파티션 구독을 재등록했습니다. partitionId={}", partitionId);
		} catch (Exception ex) {
			log.warn("파티션 구독 재등록 실패, 다음 스케줄에서 재시도합니다. partitionId={}", partitionId, ex);
		}
	}

	private boolean isBusyGroupError(Throwable throwable) {
		return hasRedisErrorToken(throwable, "BUSYGROUP");
	}

	private boolean isNoGroupError(Throwable throwable) {
		return hasRedisErrorToken(throwable, "NOGROUP");
	}

	private boolean hasRedisErrorToken(@Nullable
	Throwable throwable, String token) {
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.toUpperCase().contains(token)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private void registerPartitionPendingGauge(int partitionId, String streamKey) {
		if (meterRegistry == null) {
			return;
		}
		partitionPendingMessages.computeIfAbsent(partitionId, id -> {
			AtomicLong gaugeValue = new AtomicLong();
			MetricLabelPolicy.validate(
				"chat_stream_partition_pending_messages",
				"partitionId",
				String.valueOf(id),
				"streamKey",
				streamKey);
			meterRegistry.gauge(
				"chat_stream_partition_pending_messages",
				Tags.of("partitionId", String.valueOf(id), "streamKey", streamKey),
				gaugeValue);
			return gaugeValue;
		});
	}

	private void updatePendingBacklog(@Nullable
	Integer partitionId, String streamKey, long totalPending) {
		if (partitionId == null || meterRegistry == null) {
			return;
		}
		registerPartitionPendingGauge(partitionId, streamKey);
		AtomicLong gaugeValue = partitionPendingMessages.get(partitionId);
		if (gaugeValue != null) {
			gaugeValue.set(totalPending);
		}
	}

	private void recordSuccessMetrics(String partitionId, String streamKey, Duration latency) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate(
			"chat_stream_partition_messages_total",
			"partitionId",
			partitionId,
			"streamKey",
			streamKey);
		meterRegistry.counter(
			"chat_stream_partition_messages_total",
			"partitionId",
			partitionId,
			"streamKey",
			streamKey).increment();

		MetricLabelPolicy.validate(
			"chat_stream_partition_processing_latency",
			"partitionId",
			partitionId,
			"streamKey",
			streamKey);
		meterRegistry.timer(
			"chat_stream_partition_processing_latency",
			"partitionId",
			partitionId,
			"streamKey",
			streamKey).record(latency);
	}

	private void recordErrorMetric(String partitionId, String streamKey, String errorType) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate(
			"chat_stream_partition_errors_total",
			"partitionId",
			partitionId,
			"streamKey",
			streamKey,
			"errorType",
			errorType);
		meterRegistry.counter(
			"chat_stream_partition_errors_total",
			"partitionId",
			partitionId,
			"streamKey",
			streamKey,
			"errorType",
			errorType).increment();
	}

	@Nullable
	private Integer resolvePartitionId(@Nullable
	String streamKey) {
		if (streamKey == null || !streamKey.startsWith(PARTITION_STREAM_PREFIX)) {
			return null;
		}
		String partitionText = streamKey.substring(PARTITION_STREAM_PREFIX.length());
		try {
			return Integer.valueOf(partitionText);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private String partitionLabel(@Nullable
	Integer partitionId) {
		return partitionId == null ? "legacy" : String.valueOf(partitionId);
	}

	private enum EnsureGroupResult {
		CREATED,
		ALREADY_EXISTS
	}
}
