package com.tasteam.domain.subgroup.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubgroupListResponse {

	private List<SubgroupListItem> data;
	private PageInfo page;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PageInfo {
		private String sort;
		private String nextCursor;
		private Integer size;
		private Boolean hasNext;
	}
}
