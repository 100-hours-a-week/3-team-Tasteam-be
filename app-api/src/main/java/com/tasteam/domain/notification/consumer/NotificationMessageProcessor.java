package com.tasteam.domain.notification.consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tasteam.domain.notification.dispatch.NotificationDispatcher;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;
import com.tasteam.global.aop.ObservedMqProcess;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class NotificationMessageProcessor {

	private final NotificationDispatcher dispatcher;

	@ObservedMqProcess(domain = "notification", metricPrefix = "notification.consumer")
	public void process(NotificationRequestedPayload payload) {
		dispatcher.dispatch(payload);
	}
}
