package com.tasteam.domain.search.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
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
			List<MemberSearchHistory> existingList = memberSearchHistoryRepository
				.findAllByMemberIdAndKeywordAndDeletedAtIsNull(memberId, keyword);

			if (!existingList.isEmpty()) {
				existingList.get(0).incrementCount();
				if (existingList.size() > 1) {
					existingList.subList(1, existingList.size()).forEach(MemberSearchHistory::delete);
				}
			} else {
				memberSearchHistoryRepository.save(MemberSearchHistory.create(memberId, keyword));
			}
		} catch (DataIntegrityViolationException ex) {
			handleDuplicateInsert(memberId, keyword);
		} catch (Exception ex) {
			log.warn("검색 히스토리 업데이트에 실패했습니다: {}", ex.getMessage());
		}
	}

	private void handleDuplicateInsert(Long memberId, String keyword) {
		try {
			List<MemberSearchHistory> existingList = memberSearchHistoryRepository
				.findAllByMemberIdAndKeywordAndDeletedAtIsNull(memberId, keyword);
			if (!existingList.isEmpty()) {
				existingList.get(0).incrementCount();
			}
		} catch (Exception ex) {
			log.warn("검색 히스토리 재시도 실패: {}", ex.getMessage());
		}
	}
}
