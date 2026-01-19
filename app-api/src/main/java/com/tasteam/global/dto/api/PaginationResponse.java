package com.tasteam.global.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationResponse {
	private Integer page;
	private Integer size;
	private Integer totalPages;
	private Long totalElements;
	private String nextCursor;
	private Boolean hasNext;
}
