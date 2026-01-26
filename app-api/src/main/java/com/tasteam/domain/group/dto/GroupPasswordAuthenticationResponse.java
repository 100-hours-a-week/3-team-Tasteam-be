package com.tasteam.domain.group.dto;

import java.time.Instant;

public record GroupPasswordAuthenticationResponse(
	Boolean verified,
	Instant joinedAt) {
}
