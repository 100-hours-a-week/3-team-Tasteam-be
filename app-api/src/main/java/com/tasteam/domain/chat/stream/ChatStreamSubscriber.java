package com.tasteam.domain.chat.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.repository.ChatRoomRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ChatStreamSubscriber {
	private static final Duration POLL_MIN_IDLE = Duration.ofMinutes(1);
	private static final int PENDING_SCAN_COUNT = 100;
	private static final int MAX_RETRY_COUNT = 3;
	private static final int MAX_STREAM_LENGTH = 1000;

	private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
	private final StringRedisTemplate stringRedisTemplate;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatStreamKeyResolver keyResolver;
	private final ChatStreamGroupNameProvider groupNameProvider;
	private final SimpMessagingTemplate messagingTemplate;

	private final Map<Long, Subscription> subscriptions = new ConcurrentHashMap<>();

	@PostConstruct
	public void start() {
		container.start();
		refreshSubscriptions();
	}

	@PreDestroy
	public void stop() {
		container.stop();
	}

	@Scheduled(fixedDelayString = "PT30S")
	public void refreshSubscriptions() {
		Set<Long> currentRoomIds = Set.copyOf(chatRoomRepository.findActiveRoomIds());
		for (Long roomId : currentRoomIds) {
			subscriptions.computeIfAbsent(roomId, this::registerRoomSubscription);
		}
	}

	@Scheduled(fixedDelayString = "PT30S")
	public void recoverPendingMessages() {
		StreamOperations<String, String, String> streamOperations = stringRedisTemplate.opsForStream();
		for (Long roomId : subscriptions.keySet()) {
			String streamKey = keyResolver.roomStreamKey(roomId);
			PendingMessages pending = streamOperations.pending(
				streamKey,
				groupNameProvider.groupName(),
				Range.unbounded(),
				PENDING_SCAN_COUNT);
			if (pending == null || pending.isEmpty()) {
				continue;
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

			claimAndHandle(streamKey, toRetry, false);
			claimAndHandle(streamKey, toDeadLetter, true);
		}
	}

	private Subscription registerRoomSubscription(Long roomId) {
		String streamKey = keyResolver.roomStreamKey(roomId);
		ensureGroupExists(streamKey);
		Consumer consumer = Consumer.from(groupNameProvider.groupName(), groupNameProvider.consumerName());
		return container.receive(consumer, StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
			this::handleRecord);
	}

	private void handleRecord(MapRecord<String, String, String> record) {
		try {
			ChatStreamPayload payload = ChatStreamPayload.fromMap(record.getValue());
			ChatMessageItemResponse itemResponse = payload.toMessageItem();
			messagingTemplate.convertAndSend("/topic/chat-rooms/" + payload.chatRoomId(), itemResponse);
			ack(record);
		} catch (Exception ex) {
			log.warn("Failed to process chat stream record. stream={}, id={}", record.getStream(), record.getId(), ex);
		}
	}

	private void claimAndHandle(String streamKey, List<RecordId> recordIds, boolean deadLetter) {
		if (recordIds.isEmpty()) {
			return;
		}
		StreamOperations<String, String, String> streamOperations = stringRedisTemplate.opsForStream();
		XClaimOptions claimOptions = XClaimOptions.minIdle(POLL_MIN_IDLE).ids(recordIds.toArray(RecordId[]::new));
		List<MapRecord<String, String, String>> claimed = streamOperations.claim(
			streamKey,
			groupNameProvider.groupName(),
			groupNameProvider.consumerName(),
			claimOptions);
		if (claimed == null || claimed.isEmpty()) {
			return;
		}
		for (MapRecord<String, String, String> record : claimed) {
			if (deadLetter) {
				sendToDeadLetter(record);
				ack(record);
			} else {
				handleRecord(record);
			}
		}
	}

	private void sendToDeadLetter(MapRecord<String, String, String> record) {
		String deadLetterKey = keyResolver.deadLetterStreamKey();
		MapRecord<String, String, String> dlqRecord = StreamRecords.newRecord()
			.in(deadLetterKey)
			.ofMap(record.getValue());
		stringRedisTemplate.opsForStream().add(
			dlqRecord,
			org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions
				.maxlen(MAX_STREAM_LENGTH)
				.approximateTrimming(true));
	}

	private void ack(MapRecord<String, String, String> record) {
		stringRedisTemplate.opsForStream().acknowledge(
			record.getStream(),
			groupNameProvider.groupName(),
			record.getId());
	}

	private void ensureGroupExists(String streamKey) {
		stringRedisTemplate.execute((RedisCallback<Void>)connection -> {
			try {
				connection.streamCommands().xGroupCreate(
					stringRedisTemplate.getStringSerializer().serialize(streamKey),
					groupNameProvider.groupName(),
					ReadOffset.latest(),
					true);
			} catch (Exception ex) {
				String message = ex.getMessage();
				if (message == null || !message.contains("BUSYGROUP")) {
					throw ex;
				}
			}
			return null;
		});
	}
}
