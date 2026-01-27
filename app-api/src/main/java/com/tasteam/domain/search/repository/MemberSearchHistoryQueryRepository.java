package com.tasteam.domain.search.repository;

import java.util.List;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.entity.MemberSearchHistory;

public interface MemberSearchHistoryQueryRepository {

	List<MemberSearchHistory> findRecentSearches(Long memberId, SearchCursor cursor, int size);
}
