package com.tasteam.domain.group.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupEmailAuthenticationRequest(
	@NotBlank
	String code) {
}
