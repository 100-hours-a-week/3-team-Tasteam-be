package com.tasteam.domain.group.dto;

import java.time.Instant;

public record GroupEmailAuthenticationResponse(
	Boolean verified,
	Instant joinedAt) {
}
