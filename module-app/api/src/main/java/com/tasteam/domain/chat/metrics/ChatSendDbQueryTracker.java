package com.tasteam.domain.chat.metrics;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ChatSendDbQueryTracker {

	@Nullable
	private final Counter postgresSelectCounter;
	@Nullable
	private final Counter postgresInsertCounter;

	private final ThreadLocal<RequestCounter> requestCounter = new ThreadLocal<>();

	public ChatSendDbQueryTracker(@Nullable
	MeterRegistry meterRegistry) {
		if (meterRegistry == null) {
			this.postgresSelectCounter = null;
			this.postgresInsertCounter = null;
			return;
		}
		this.postgresSelectCounter = meterRegistry.counter("postgres_select_total");
		this.postgresInsertCounter = meterRegistry.counter("postgres_insert_total");
	}

	public TrackingContext startTracking() {
		RequestCounter previous = requestCounter.get();
		RequestCounter current = new RequestCounter();
		requestCounter.set(current);
		return new TrackingContext(previous, current);
	}

	public DbQuerySnapshot stopTracking(TrackingContext context) {
		RequestCounter current = context.current;
		if (context.previous == null) {
			requestCounter.remove();
		} else {
			requestCounter.set(context.previous);
		}
		if (current == null) {
			return DbQuerySnapshot.empty();
		}
		return new DbQuerySnapshot(current.total, current.select, current.insert);
	}

	public void recordSql(@Nullable
	String sql) {
		QueryKind queryKind = classify(sql);
		if (queryKind == QueryKind.SELECT && postgresSelectCounter != null) {
			postgresSelectCounter.increment();
		}
		if (queryKind == QueryKind.INSERT && postgresInsertCounter != null) {
			postgresInsertCounter.increment();
		}

		RequestCounter current = requestCounter.get();
		if (current == null) {
			return;
		}
		current.total++;
		if (queryKind == QueryKind.SELECT) {
			current.select++;
		}
		if (queryKind == QueryKind.INSERT) {
			current.insert++;
		}
	}

	private QueryKind classify(@Nullable
	String sql) {
		if (sql == null || sql.isBlank()) {
			return QueryKind.OTHER;
		}

		String normalized = skipLeadingTrivia(sql).toLowerCase(java.util.Locale.ROOT);
		if (normalized.startsWith("select")) {
			return QueryKind.SELECT;
		}
		if (normalized.startsWith("insert")) {
			return QueryKind.INSERT;
		}
		return QueryKind.OTHER;
	}

	private String skipLeadingTrivia(String sql) {
		int length = sql.length();
		int index = 0;

		while (index < length) {
			char current = sql.charAt(index);
			if (Character.isWhitespace(current)) {
				index++;
				continue;
			}

			if (current == '-' && index + 1 < length && sql.charAt(index + 1) == '-') {
				index += 2;
				while (index < length && sql.charAt(index) != '\n') {
					index++;
				}
				continue;
			}

			if (current == '/' && index + 1 < length && sql.charAt(index + 1) == '*') {
				index += 2;
				while (index + 1 < length && !(sql.charAt(index) == '*' && sql.charAt(index + 1) == '/')) {
					index++;
				}
				if (index + 1 < length) {
					index += 2;
				}
				continue;
			}

			break;
		}

		return index >= length ? "" : sql.substring(index);
	}

	private enum QueryKind {
		SELECT,
		INSERT,
		OTHER
	}

	private static final class RequestCounter {
		private long total;
		private long select;
		private long insert;
	}

	public static final class TrackingContext {
		@Nullable
		private final RequestCounter previous;
		private final RequestCounter current;

		private TrackingContext(@Nullable
		RequestCounter previous, RequestCounter current) {
			this.previous = previous;
			this.current = current;
		}
	}

	public record DbQuerySnapshot(
		long total,
		long select,
		long insert) {
		private static DbQuerySnapshot empty() {
			return new DbQuerySnapshot(0L, 0L, 0L);
		}
	}
}
