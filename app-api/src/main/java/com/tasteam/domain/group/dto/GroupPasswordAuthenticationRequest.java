package com.tasteam.domain.group.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupPasswordAuthenticationRequest(
	@NotBlank
	String code) {
}
