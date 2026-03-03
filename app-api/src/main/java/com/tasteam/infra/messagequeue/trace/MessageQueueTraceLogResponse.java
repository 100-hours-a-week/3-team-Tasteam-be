package com.tasteam.infra.messagequeue.trace;

import java.time.Instant;

public record MessageQueueTraceLogResponse(
	Long id,
	String messageId,
	String topic,
	String provider,
	String messageKey,
	String consumerGroup,
	String stage,
	Long processingMillis,
	String errorMessage,
	Instant createdAt) {

	public static MessageQueueTraceLogResponse from(MessageQueueTraceLog traceLog) {
		return new MessageQueueTraceLogResponse(
			traceLog.getId(),
			traceLog.getMessageId(),
			traceLog.getTopic(),
			traceLog.getProvider(),
			traceLog.getMessageKey(),
			traceLog.getConsumerGroup(),
			traceLog.getStage().name(),
			traceLog.getProcessingMillis(),
			traceLog.getErrorMessage(),
			traceLog.getCreatedAt());
	}
}
