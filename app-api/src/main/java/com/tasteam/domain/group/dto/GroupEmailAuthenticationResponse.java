package com.tasteam.domain.group.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEmailAuthenticationResponse {

	private Boolean verified;
	private Instant joinedAt;
}
