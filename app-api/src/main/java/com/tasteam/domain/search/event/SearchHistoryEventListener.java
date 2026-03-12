package com.tasteam.domain.search.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHistoryEventListener {

	private final MemberSearchHistoryRepository memberSearchHistoryRepository;

	@Async("searchHistoryExecutor")
	@Transactional
	@EventListener
	public void onSearchCompleted(SearchCompletedEvent event) {
		if (event.memberId() == null) {
			return;
		}
		if (event.groupResultCount() == 0 && event.restaurantResultCount() == 0) {
			return;
		}
		try {
			memberSearchHistoryRepository.upsertSearchHistory(event.memberId(), event.keyword());
		} catch (Exception ex) {
			log.warn("검색 히스토리 업데이트에 실패했습니다: {}", ex.getMessage());
		}
	}
}
