package com.tasteam.global.health.controller;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.health.controller.docs.HealthCheckApiDocs;
import com.tasteam.global.health.dto.response.HealthCheckResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/health")
public class HealthCheckController implements HealthCheckApiDocs {

	private final HealthEndpoint healthEndpoint;

	@GetMapping
	public SuccessResponse<HealthCheckResponse> check() {
		HealthComponent health = healthEndpoint.health();
		return SuccessResponse.success(HealthCheckResponse.from(health));
	}

}
