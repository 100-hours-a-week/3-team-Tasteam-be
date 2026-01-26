package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.tasteam.domain.group.entity.Group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateResponse {

	private Long id;
	private String status;
	private Instant createdAt;

	public static GroupCreateResponse from(Group group) {
		return GroupCreateResponse.builder()
			.id(group.getId())
			.status(group.getStatus().name())
			.createdAt(group.getCreatedAt())
			.build();
	}
}
