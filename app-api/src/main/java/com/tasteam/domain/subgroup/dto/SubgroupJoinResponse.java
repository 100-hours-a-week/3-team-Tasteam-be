package com.tasteam.domain.subgroup.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubgroupJoinResponse {

	private JoinData data;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class JoinData {
		private Long subgroupId;
		private Instant joinedAt;
	}
}
