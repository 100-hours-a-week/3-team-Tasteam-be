package com.tasteam.domain.chat.stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ChatStreamPublisher {
	private static final int MAX_STREAM_LENGTH = 1000;

	private final StringRedisTemplate stringRedisTemplate;
	private final ChatStreamKeyResolver keyResolver;

	public void publish(Long chatRoomId, ChatMessageItemResponse message) {
		String streamKey = keyResolver.roomStreamKey(chatRoomId);
		ChatStreamPayload payload = ChatStreamPayload.from(chatRoomId, message);
		MapRecord<String, String, String> record = StreamRecords.newRecord()
			.in(streamKey)
			.ofMap(payload.toMap());

		stringRedisTemplate.opsForStream().add(
			record,
			XAddOptions.maxlen(MAX_STREAM_LENGTH).approximateTrimming(true));
	}
}
