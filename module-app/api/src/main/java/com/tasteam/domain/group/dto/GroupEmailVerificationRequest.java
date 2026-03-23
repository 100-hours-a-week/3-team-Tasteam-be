package com.tasteam.domain.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GroupEmailVerificationRequest(
	@NotBlank @Email
	String email) {
}
