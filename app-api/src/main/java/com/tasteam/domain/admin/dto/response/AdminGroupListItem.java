package com.tasteam.domain.admin.dto.response;

import java.time.Instant;

import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.group.type.GroupType;

public record AdminGroupListItem(
	Long id,
	String name,
	GroupType type,
	String address,
	GroupJoinType joinType,
	GroupStatus status,
	Instant createdAt) {
}
