package com.tasteam.domain.group.dto;

import java.time.Instant;

public record GroupEmailVerificationResponse(
	Instant expiresAt) {
}
