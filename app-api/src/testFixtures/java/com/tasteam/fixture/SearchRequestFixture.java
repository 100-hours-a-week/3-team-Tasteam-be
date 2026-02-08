package com.tasteam.fixture;

import com.tasteam.domain.search.dto.request.SearchRequest;

public final class SearchRequestFixture {

	public static final String DEFAULT_KEYWORD = "맛집";
	public static final Integer DEFAULT_SIZE = 20;

	private SearchRequestFixture() {}

	public static SearchRequest createRequest() {
		return new SearchRequest(DEFAULT_KEYWORD, null, null, null, null, DEFAULT_SIZE);
	}

	public static SearchRequest createRequest(String keyword) {
		return new SearchRequest(keyword, null, null, null, null, DEFAULT_SIZE);
	}

	public static SearchRequest createRequest(String keyword, String cursor, Integer size) {
		return new SearchRequest(keyword, null, null, null, cursor, size);
	}

	public static SearchRequest createRequestWithCursor(String cursor) {
		return new SearchRequest(DEFAULT_KEYWORD, null, null, null, cursor, DEFAULT_SIZE);
	}

	public static SearchRequest createRequestWithoutKeyword() {
		return new SearchRequest(null, null, null, null, null, DEFAULT_SIZE);
	}

	public static SearchRequest createRequestWithBlankKeyword() {
		return new SearchRequest("", null, null, null, null, DEFAULT_SIZE);
	}

	public static SearchRequest createRequestWithInvalidSize() {
		return new SearchRequest(DEFAULT_KEYWORD, null, null, null, null, 101);
	}
}
