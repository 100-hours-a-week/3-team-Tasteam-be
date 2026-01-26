package com.tasteam.domain.subgroup.dto;

import java.util.List;

public record SubgroupListResponse(List<SubgroupListItem> data, PageInfo page) {

	public record PageInfo(String sort, String nextCursor, Integer size, Boolean hasNext) {
	}
}
