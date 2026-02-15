package com.tasteam.infra.messagequeue.trace;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.infra.messagequeue.MessageQueueMessage;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueTraceService {

	private final MessageQueueTraceLogRepository traceLogRepository;
	@Nullable
	private final MeterRegistry meterRegistry;

	@Transactional
	public void recordPublish(MessageQueueMessage message, MessageQueueProviderType providerType) {
		log.info("메시지큐 발행 추적. topic={}, messageId={}, provider={}",
			message.topic(), message.messageId(), providerType.value());

		saveQuietly(MessageQueueTraceLog.publish(
			message.messageId(),
			message.topic(),
			providerType.value(),
			message.key()));

		incrementCounter("mq.publish.count", message.topic(), providerType.value(), "success");
	}

	@Transactional
	public void recordConsumeSuccess(
		MessageQueueMessage message,
		MessageQueueProviderType providerType,
		String consumerGroup,
		long processingMillis) {
		log.info("메시지큐 소비 성공 추적. topic={}, messageId={}, provider={}, consumerGroup={}, processingMillis={}",
			message.topic(), message.messageId(), providerType.value(), consumerGroup, processingMillis);

		saveQuietly(MessageQueueTraceLog.consumeSuccess(
			message.messageId(),
			message.topic(),
			providerType.value(),
			message.key(),
			consumerGroup,
			processingMillis));

		incrementCounter("mq.consume.count", message.topic(), providerType.value(), "success");
		recordLatency(message.topic(), providerType.value(), processingMillis);
	}

	@Transactional
	public void recordConsumeFail(
		MessageQueueMessage message,
		MessageQueueProviderType providerType,
		String consumerGroup,
		long processingMillis,
		Exception ex) {
		log.warn("메시지큐 소비 실패 추적. topic={}, messageId={}, provider={}, consumerGroup={}, processingMillis={}",
			message.topic(), message.messageId(), providerType.value(), consumerGroup, processingMillis, ex);

		saveQuietly(MessageQueueTraceLog.consumeFail(
			message.messageId(),
			message.topic(),
			providerType.value(),
			message.key(),
			consumerGroup,
			processingMillis,
			ex.getMessage()));

		incrementCounter("mq.consume.count", message.topic(), providerType.value(), "fail");
		recordLatency(message.topic(), providerType.value(), processingMillis);
	}

	@Transactional(readOnly = true)
	public List<MessageQueueTraceLog> findRecent(@Nullable
	String messageId, int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
		if (messageId != null && !messageId.isBlank()) {
			return traceLogRepository.findAllByMessageIdOrderByIdDesc(messageId, pageRequest).getContent();
		}
		return traceLogRepository.findAllByOrderByIdDesc(pageRequest).getContent();
	}

	private void saveQuietly(MessageQueueTraceLog traceLog) {
		try {
			traceLogRepository.save(traceLog);
		} catch (Exception ex) {
			log.error("메시지큐 추적 로그 저장에 실패했습니다. messageId={}, topic={}",
				traceLog.getMessageId(), traceLog.getTopic(), ex);
		}
	}

	private void incrementCounter(String metricName, String topic, String provider, String result) {
		if (meterRegistry == null) {
			return;
		}
		meterRegistry.counter(metricName, "topic", topic, "provider", provider, "result", result).increment();
	}

	private void recordLatency(String topic, String provider, long processingMillis) {
		if (meterRegistry == null) {
			return;
		}
		Timer.builder("mq.consume.latency")
			.tag("topic", topic)
			.tag("provider", provider)
			.register(meterRegistry)
			.record(java.time.Duration.ofMillis(processingMillis));
	}
}
