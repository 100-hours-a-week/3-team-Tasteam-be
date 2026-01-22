package com.tasteam.domain.search.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.search.dto.request.SearchRequest;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.domain.search.service.SearchService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

	private final SearchService searchService;

	@PostMapping
	public SuccessResponse<SearchResponse> search(
		@CurrentUser
		Long memberId,
		@Valid @RequestBody
		SearchRequest request) {
		return SuccessResponse.success(searchService.search(memberId, request));
	}
}
