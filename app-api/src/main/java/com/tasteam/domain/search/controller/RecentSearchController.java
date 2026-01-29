package com.tasteam.domain.search.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.search.dto.response.RecentSearchItem;
import com.tasteam.domain.search.service.SearchService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recent-searches")
@RequiredArgsConstructor
public class RecentSearchController {

	private final SearchService searchService;

	@GetMapping
	public SuccessResponse<OffsetPageResponse<RecentSearchItem>> getRecentSearches(
		@CurrentUser
		Long memberId) {
		return SuccessResponse.success(searchService.getRecentSearches(memberId));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteRecentSearch(
		@CurrentUser
		Long memberId,
		@PathVariable
		Long id) {
		searchService.deleteRecentSearch(memberId, id);
		return ResponseEntity.noContent().build();
	}
}
