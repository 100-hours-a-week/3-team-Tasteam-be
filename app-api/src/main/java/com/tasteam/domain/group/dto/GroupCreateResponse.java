package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.tasteam.domain.group.entity.Group;

public record GroupCreateResponse(
	Long id,
	String status,
	Instant createdAt) {

	public static GroupCreateResponse from(Group group) {
		return new GroupCreateResponse(
			group.getId(),
			group.getStatus().name(),
			group.getCreatedAt());
	}
}
