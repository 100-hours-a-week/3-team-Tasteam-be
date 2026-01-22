package com.tasteam.domain.search.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.search.dto.request.RecentSearchQueryParams;
import com.tasteam.domain.search.dto.response.RecentSearchListResponse;
import com.tasteam.domain.search.service.SearchService;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/me/recent-searches")
@RequiredArgsConstructor
public class RecentSearchController {

	private final SearchService searchService;

	@GetMapping
	public SuccessResponse<RecentSearchListResponse> getRecentSearches(
		@RequestHeader("X-Member-Id")
		Long memberId,
		@Valid @ModelAttribute
		RecentSearchQueryParams params) {
		return SuccessResponse.success(searchService.getRecentSearches(memberId, params));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteRecentSearch(
		@RequestHeader("X-Member-Id")
		Long memberId,
		@PathVariable
		Long id) {
		searchService.deleteRecentSearch(memberId, id);
		return ResponseEntity.noContent().build();
	}
}
