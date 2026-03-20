package com.tasteam.global.health.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;

public record HealthCheckResponse(
	String status,
	Instant checkedAt,
	List<HealthComponentStatus> components) {

	public static HealthCheckResponse from(HealthComponent root) {
		return new HealthCheckResponse(
			root.getStatus().getCode(),
			Instant.now(),
			extractComponents(root));
	}

	private static List<HealthComponentStatus> extractComponents(HealthComponent root) {
		if (root instanceof CompositeHealth composite) {
			Map<String, HealthComponent> children = composite.getComponents();

			if (children == null || children.isEmpty()) {
				return List.of(HealthComponentStatus.from("self", root));
			}

			return children.entrySet().stream()
				.map(entry -> HealthComponentStatus.from(entry.getKey(), entry.getValue()))
				.toList();
		}

		return List.of(HealthComponentStatus.from("self", root));
	}

}
