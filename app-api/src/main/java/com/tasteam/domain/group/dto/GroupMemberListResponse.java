package com.tasteam.domain.group.dto;

import java.util.List;

public record GroupMemberListResponse(
	List<GroupMemberListItem> data,
	PageInfo page) {

	public record PageInfo(String nextCursor, Integer size, Boolean hasNext) {
	}
}
