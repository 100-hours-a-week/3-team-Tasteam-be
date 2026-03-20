package com.tasteam.global.health.dto.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.Status;

public record HealthComponentStatus(
	String name,
	String status,
	Map<String, Object> details) {

	private static final Map<String, Object> EMPTY_DETAILS = Collections.emptyMap();

	public static HealthComponentStatus from(String name, HealthComponent component) {
		return new HealthComponentStatus(
			name,
			component.getStatus().getCode(),
			normalizeDetails(component));
	}

	private static Map<String, Object> normalizeDetails(HealthComponent component) {
		if (component instanceof Health health) {
			return normalizeMap(health.getDetails());
		}

		if (component instanceof CompositeHealth composite) {
			return normalizeMap(composite.getDetails());
		}

		return EMPTY_DETAILS;
	}

	private static Map<String, Object> normalizeMap(Map<?, ?> source) {
		if (source == null || source.isEmpty()) {
			return EMPTY_DETAILS;
		}

		Map<String, Object> normalized = new LinkedHashMap<>(source.size());
		source.forEach((key, value) -> normalized.put(String.valueOf(key), normalizeValue(value)));
		return Collections.unmodifiableMap(normalized);
	}

	private static Object normalizeValue(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof HealthComponent healthComponent) {
			return Map.of(
				"status", healthComponent.getStatus().getCode(),
				"details", normalizeDetails(healthComponent));
		}

		if (value instanceof Status status) {
			return status.getCode();
		}

		if (value instanceof Map<?, ?> mapValue) {
			return normalizeMap(mapValue);
		}

		if (value instanceof List<?> listValue) {
			return listValue.stream()
				.map(HealthComponentStatus::normalizeValue)
				.toList();
		}

		return value;
	}

}
