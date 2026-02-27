package com.tasteam.domain.chat.ws;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChatWebSocketObservabilityListener {

	private static final Duration RECONNECT_WINDOW = Duration.ofMinutes(1);

	private final MeterRegistry meterRegistry;
	private final Timer sessionLifetimeTimer;
	private final AtomicInteger activeConnections = new AtomicInteger();
	private final ConcurrentMap<String, Long> sessionMemberIdBySessionId = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Instant> connectedAtBySessionId = new ConcurrentHashMap<>();
	private final ConcurrentMap<Long, Instant> lastDisconnectedAtByMemberId = new ConcurrentHashMap<>();

	public ChatWebSocketObservabilityListener(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.sessionLifetimeTimer = Timer.builder("ws.session.lifetime")
			.publishPercentileHistogram()
			.register(meterRegistry);
		meterRegistry.gauge("ws.connections.active", Tags.empty(), activeConnections);
	}

	@EventListener
	public void onSessionConnected(SessionConnectedEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		Long memberId = resolveMemberId(accessor);
		Instant now = Instant.now();

		activeConnections.incrementAndGet();
		meterRegistry.counter("ws.connect.total").increment();
		if (sessionId != null) {
			connectedAtBySessionId.put(sessionId, now);
			if (memberId != null) {
				sessionMemberIdBySessionId.put(sessionId, memberId);
			}
		}

		if (memberId != null) {
			Instant lastDisconnectedAt = lastDisconnectedAtByMemberId.get(memberId);
			if (lastDisconnectedAt != null
				&& Duration.between(lastDisconnectedAt, now).compareTo(RECONNECT_WINDOW) <= 0) {
				meterRegistry.counter("ws.reconnect.total").increment();
				log.info("WS RECONNECT detected. memberId={}, sessionId={}", memberId, sessionId);
			}
		}

		log.info("WS CONNECTED. memberId={}, sessionId={}, activeConnections={}",
			memberId, sessionId, activeConnections.get());
	}

	@EventListener
	public void onSessionDisconnected(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		Long memberId = sessionId != null
			? sessionMemberIdBySessionId.remove(sessionId)
			: resolveMemberId(accessor);

		DisconnectReason disconnectReason = classify(event.getCloseStatus());
		int current = activeConnections.updateAndGet(value -> Math.max(0, value - 1));
		meterRegistry.counter("ws.disconnect.total").increment();
		meterRegistry.counter("ws.disconnect.by.reason.total", "reason", disconnectReason.value()).increment();
		if (disconnectReason == DisconnectReason.HEARTBEAT_TIMEOUT) {
			meterRegistry.counter("ws.heartbeat.timeout.total").increment();
		}

		Instant connectedAt = sessionId != null ? connectedAtBySessionId.remove(sessionId) : null;
		if (connectedAt != null) {
			Duration lifetime = Duration.between(connectedAt, Instant.now());
			sessionLifetimeTimer.record(lifetime);
		}

		if (memberId != null) {
			lastDisconnectedAtByMemberId.put(memberId, Instant.now());
		}

		CloseStatus closeStatus = event.getCloseStatus();
		log.info(
			"WS DISCONNECTED. memberId={}, sessionId={}, reason={}, closeCode={}, closeReason={}, activeConnections={}",
			memberId,
			sessionId,
			disconnectReason.value(),
			closeStatus != null ? closeStatus.getCode() : null,
			closeStatus != null ? closeStatus.getReason() : null,
			current);
	}

	private Long resolveMemberId(StompHeaderAccessor accessor) {
		Principal user = accessor.getUser();
		if (user == null || user.getName() == null) {
			return null;
		}
		try {
			return Long.valueOf(user.getName());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private DisconnectReason classify(CloseStatus closeStatus) {
		if (closeStatus == null) {
			return DisconnectReason.UNKNOWN;
		}

		int closeCode = closeStatus.getCode();
		String reason = closeStatus.getReason();
		String normalizedReason = reason == null ? "" : reason.toLowerCase(Locale.ROOT);

		if (normalizedReason.contains("heartbeat") || closeCode == CloseStatus.SESSION_NOT_RELIABLE.getCode()) {
			return DisconnectReason.HEARTBEAT_TIMEOUT;
		}

		if (normalizedReason.contains("proxy")
			|| normalizedReason.contains("caddy")
			|| normalizedReason.contains("idle timeout")) {
			return DisconnectReason.PROXY_TIMEOUT;
		}

		if (closeCode == CloseStatus.NORMAL.getCode()
			|| closeCode == CloseStatus.GOING_AWAY.getCode()) {
			return DisconnectReason.CLIENT_CLOSE;
		}

		if (closeCode == CloseStatus.SERVICE_RESTARTED.getCode()) {
			return DisconnectReason.SERVER_SHUTDOWN;
		}

		if (closeCode == CloseStatus.NO_CLOSE_FRAME.getCode()) {
			return DisconnectReason.NETWORK_ERROR;
		}

		return DisconnectReason.UNKNOWN;
	}

	private enum DisconnectReason {
		HEARTBEAT_TIMEOUT("heartbeat_timeout"),
		CLIENT_CLOSE("client_close"),
		SERVER_SHUTDOWN("server_shutdown"),
		PROXY_TIMEOUT("proxy_timeout"),
		NETWORK_ERROR("network_error"),
		UNKNOWN("unknown");

		private final String value;

		DisconnectReason(String value) {
			this.value = value;
		}

		public String value() {
			return value;
		}
	}
}
