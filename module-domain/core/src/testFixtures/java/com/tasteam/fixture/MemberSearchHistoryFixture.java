package com.tasteam.fixture;

import com.tasteam.domain.search.entity.MemberSearchHistory;

public final class MemberSearchHistoryFixture {

	public static final Long DEFAULT_MEMBER_ID = 1L;
	public static final String DEFAULT_KEYWORD = "테스트검색어";

	private MemberSearchHistoryFixture() {}

	public static MemberSearchHistory create() {
		return MemberSearchHistory.create(DEFAULT_MEMBER_ID, DEFAULT_KEYWORD);
	}

	public static MemberSearchHistory create(Long memberId, String keyword) {
		return MemberSearchHistory.create(memberId, keyword);
	}
}
