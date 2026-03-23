package com.tasteam.domain.chat.stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.tasteam.domain.chat.config.ChatStreamProperties;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ChatStreamPublisher {
	private static final int MAX_STREAM_LENGTH = 1000;

	private final StringRedisTemplate stringRedisTemplate;
	private final ChatStreamKeyResolver keyResolver;
	private final ChatStreamProperties chatStreamProperties;

	public void publish(Long chatRoomId, ChatMessageItemResponse message) {
		ChatStreamPayload payload = ChatStreamPayload.from(chatRoomId, message);
		if (chatStreamProperties.partitionConsumeEnabled()) {
			int partitionId = keyResolver.resolvePartition(chatRoomId, chatStreamProperties.partitionCount());
			publishTo(keyResolver.partitionStreamKey(partitionId), payload);
			if (chatStreamProperties.dualWriteEnabled()) {
				publishTo(keyResolver.roomStreamKey(chatRoomId), payload);
			}
			return;
		}
		publishTo(keyResolver.roomStreamKey(chatRoomId), payload);
	}

	private void publishTo(String streamKey, ChatStreamPayload payload) {
		MapRecord<String, String, String> record = StreamRecords.newRecord().in(streamKey).ofMap(payload.toMap());
		stringRedisTemplate.opsForStream().add(
			record,
			XAddOptions.maxlen(MAX_STREAM_LENGTH).approximateTrimming(true));
	}
}
