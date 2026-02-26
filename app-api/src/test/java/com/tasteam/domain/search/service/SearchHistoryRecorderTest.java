package com.tasteam.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;

@UnitTest
@DisplayName("SearchHistoryRecorder")
class SearchHistoryRecorderTest {

	@Mock
	private MemberSearchHistoryRepository memberSearchHistoryRepository;

	@InjectMocks
	private SearchHistoryRecorder searchHistoryRecorder;

	@Test
	@DisplayName("memberId가 null이면 저장 없이 스킵한다")
	void recordSearchHistory_whenMemberIdIsNull_skips() {
		searchHistoryRecorder.recordSearchHistory(null, "치킨");

		verifyNoInteractions(memberSearchHistoryRepository);
	}

	@Test
	@DisplayName("기록이 없으면 새로 생성한다")
	void recordSearchHistory_whenNoExistingRecord_savesNew() {
		given(memberSearchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(1L, "치킨"))
			.willReturn(List.of());

		searchHistoryRecorder.recordSearchHistory(1L, "치킨");

		then(memberSearchHistoryRepository).should().save(any(MemberSearchHistory.class));
	}

	@Test
	@DisplayName("동일 키워드 기록이 있으면 카운트를 증가시킨다")
	void recordSearchHistory_whenExistingRecord_incrementsCount() {
		MemberSearchHistory existing = MemberSearchHistory.create(1L, "치킨");
		given(memberSearchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(1L, "치킨"))
			.willReturn(List.of(existing));

		searchHistoryRecorder.recordSearchHistory(1L, "치킨");

		then(memberSearchHistoryRepository).should(never()).save(any());
		assertThat(existing.getCount()).isEqualTo(2L);
	}

	@Test
	@DisplayName("중복 기록이 여러 개이면 첫 번째만 카운트업하고 나머지는 삭제한다")
	void recordSearchHistory_whenDuplicateRecords_incrementsFirstAndDeletesRest() {
		MemberSearchHistory h1 = MemberSearchHistory.create(1L, "치킨");
		MemberSearchHistory h2 = MemberSearchHistory.create(1L, "치킨");
		given(memberSearchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(1L, "치킨"))
			.willReturn(List.of(h1, h2));

		searchHistoryRecorder.recordSearchHistory(1L, "치킨");

		assertThat(h1.getCount()).isEqualTo(2L);
		assertThat(h2.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("예외가 발생해도 전파되지 않는다")
	void recordSearchHistory_whenExceptionOccurs_doesNotPropagate() {
		given(memberSearchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(1L, "치킨"))
			.willThrow(new RuntimeException("DB 오류"));

		assertThatCode(() -> searchHistoryRecorder.recordSearchHistory(1L, "치킨"))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("DataIntegrityViolationException 발생 시 재조회 후 카운트업을 시도한다")
	void recordSearchHistory_whenDataIntegrityViolation_retriesAndIncrements() {
		MemberSearchHistory existing = MemberSearchHistory.create(1L, "치킨");
		given(memberSearchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(1L, "치킨"))
			.willThrow(new DataIntegrityViolationException("unique violation"))
			.willReturn(List.of(existing));

		assertThatCode(() -> searchHistoryRecorder.recordSearchHistory(1L, "치킨"))
			.doesNotThrowAnyException();

		assertThat(existing.getCount()).isEqualTo(2L);
	}
}
