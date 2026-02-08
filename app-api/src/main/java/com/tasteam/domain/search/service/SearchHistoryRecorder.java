package com.tasteam.domain.search.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHistoryRecorder {

	private final MemberSearchHistoryRepository memberSearchHistoryRepository;

	@Async("searchHistoryExecutor")
	@Transactional
	public void recordSearchHistory(Long memberId, String keyword) {
		if (memberId == null) {
			return;
		}
		try {
			var existing = memberSearchHistoryRepository
				.findByMemberIdAndKeywordAndDeletedAtIsNull(memberId, keyword);

			if (existing.isPresent()) {
				existing.get().incrementCount();
			} else {
				memberSearchHistoryRepository.save(
					MemberSearchHistory.create(memberId, keyword));
			}
		} catch (Exception ex) {
			log.warn("검색 히스토리 업데이트에 실패했습니다: {}", ex.getMessage());
		}
	}
}
