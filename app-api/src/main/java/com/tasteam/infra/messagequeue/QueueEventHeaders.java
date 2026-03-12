package com.tasteam.infra.messagequeue;

import java.util.LinkedHashMap;
import java.util.Map;

public record QueueEventHeaders(Map<String, String> values) {

	public QueueEventHeaders {
		values = values == null ? Map.of() : Map.copyOf(values);
	}

	public static QueueEventHeaders empty() {
		return new QueueEventHeaders(Map.of());
	}

	public static Builder builder() {
		return new Builder();
	}

	public Map<String, String> asMap() {
		return values;
	}

	public static final class Builder {
		private final Map<String, String> values = new LinkedHashMap<>();

		public Builder eventType(String eventType) {
			values.put("eventType", eventType);
			return this;
		}

		public Builder schemaVersion(String schemaVersion) {
			values.put("schemaVersion", schemaVersion);
			return this;
		}

		public Builder put(String key, String value) {
			values.put(key, value);
			return this;
		}

		public QueueEventHeaders build() {
			return new QueueEventHeaders(values);
		}
	}
}
