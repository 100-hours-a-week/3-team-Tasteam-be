package com.tasteam.domain.group.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberListResponse {

	private List<GroupMemberListItem> data;
	private PageInfo page;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PageInfo {
		private String nextCursor;
		private Integer size;
		private Boolean hasNext;
	}
}
