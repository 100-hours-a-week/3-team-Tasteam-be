package com.tasteam.domain.subgroup.dto;

import java.time.Instant;

public record SubgroupJoinResponse(JoinData data) {

	public record JoinData(Long subgroupId, Instant joinedAt) {
	}
}
