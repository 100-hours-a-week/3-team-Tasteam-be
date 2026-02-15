package com.tasteam.infra.messagequeue.trace;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "message_queue_trace_log", indexes = {
	@Index(name = "idx_mq_trace_message_id", columnList = "message_id, id DESC"),
	@Index(name = "idx_mq_trace_topic_stage", columnList = "topic, stage, id DESC")
})
public class MessageQueueTraceLog extends BaseCreatedAtEntity {

	private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id", nullable = false, length = 100)
	private String messageId;

	@Column(name = "topic", nullable = false, length = 100)
	private String topic;

	@Column(name = "provider", nullable = false, length = 30)
	private String provider;

	@Column(name = "message_key", length = 200)
	private String messageKey;

	@Column(name = "consumer_group", length = 100)
	private String consumerGroup;

	@Enumerated(EnumType.STRING)
	@Column(name = "stage", nullable = false, length = 30)
	private MessageQueueTraceStage stage;

	@Column(name = "processing_millis")
	private Long processingMillis;

	@Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
	private String errorMessage;

	public static MessageQueueTraceLog publish(
		String messageId,
		String topic,
		String provider,
		String messageKey) {
		return MessageQueueTraceLog.builder()
			.messageId(messageId)
			.topic(topic)
			.provider(provider)
			.messageKey(messageKey)
			.stage(MessageQueueTraceStage.PUBLISH)
			.build();
	}

	public static MessageQueueTraceLog consumeSuccess(
		String messageId,
		String topic,
		String provider,
		String messageKey,
		String consumerGroup,
		long processingMillis) {
		return MessageQueueTraceLog.builder()
			.messageId(messageId)
			.topic(topic)
			.provider(provider)
			.messageKey(messageKey)
			.consumerGroup(consumerGroup)
			.stage(MessageQueueTraceStage.CONSUME_SUCCESS)
			.processingMillis(processingMillis)
			.build();
	}

	public static MessageQueueTraceLog consumeFail(
		String messageId,
		String topic,
		String provider,
		String messageKey,
		String consumerGroup,
		long processingMillis,
		String errorMessage) {
		return MessageQueueTraceLog.builder()
			.messageId(messageId)
			.topic(topic)
			.provider(provider)
			.messageKey(messageKey)
			.consumerGroup(consumerGroup)
			.stage(MessageQueueTraceStage.CONSUME_FAIL)
			.processingMillis(processingMillis)
			.errorMessage(truncateError(errorMessage))
			.build();
	}

	private static String truncateError(String errorMessage) {
		if (errorMessage == null) {
			return null;
		}
		if (errorMessage.length() <= ERROR_MESSAGE_MAX_LENGTH) {
			return errorMessage;
		}
		return errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
	}
}
