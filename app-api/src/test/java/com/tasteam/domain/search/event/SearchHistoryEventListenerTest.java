package com.tasteam.domain.search.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;

@UnitTest
@DisplayName("[유닛](Search) SearchHistoryEventListener 단위 테스트")
class SearchHistoryEventListenerTest {

	@Mock
	private MemberSearchHistoryRepository memberSearchHistoryRepository;

	@InjectMocks
	private SearchHistoryEventListener listener;

	@Test
	@DisplayName("memberId가 null이면 저장 없이 스킵한다")
	void onSearchCompleted_whenMemberIdIsNull_skips() {
		// when
		listener.onSearchCompleted(new SearchCompletedEvent(null, "치킨", 1, 1));

		// then
		verifyNoInteractions(memberSearchHistoryRepository);
	}

	@Test
	@DisplayName("검색 결과가 없으면 저장 없이 스킵한다")
	void onSearchCompleted_whenNoResults_skips() {
		// when
		listener.onSearchCompleted(new SearchCompletedEvent(1L, "치킨", 0, 0));

		// then
		verifyNoInteractions(memberSearchHistoryRepository);
	}

	@Test
	@DisplayName("검색 결과가 있으면 upsertSearchHistory를 호출한다")
	void onSearchCompleted_whenResultsExist_callsUpsert() {
		// when
		listener.onSearchCompleted(new SearchCompletedEvent(1L, "치킨", 1, 0));

		// then
		then(memberSearchHistoryRepository).should().upsertSearchHistory(1L, "치킨");
	}

	@Test
	@DisplayName("그룹 결과만 있어도 upsertSearchHistory를 호출한다")
	void onSearchCompleted_whenOnlyGroupResults_callsUpsert() {
		// when
		listener.onSearchCompleted(new SearchCompletedEvent(1L, "치킨", 3, 0));

		// then
		then(memberSearchHistoryRepository).should().upsertSearchHistory(1L, "치킨");
	}

	@Test
	@DisplayName("레스토랑 결과만 있어도 upsertSearchHistory를 호출한다")
	void onSearchCompleted_whenOnlyRestaurantResults_callsUpsert() {
		// when
		listener.onSearchCompleted(new SearchCompletedEvent(1L, "치킨", 0, 5));

		// then
		then(memberSearchHistoryRepository).should().upsertSearchHistory(1L, "치킨");
	}

	@Test
	@DisplayName("예외가 발생해도 전파되지 않는다")
	void onSearchCompleted_whenExceptionOccurs_doesNotPropagate() {
		// given
		willThrow(new RuntimeException("DB 오류"))
			.given(memberSearchHistoryRepository).upsertSearchHistory(anyLong(), anyString());

		// when & then
		assertThatCode(() -> listener.onSearchCompleted(new SearchCompletedEvent(1L, "치킨", 1, 1)))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("memberId가 null이면 upsertSearchHistory를 호출하지 않는다")
	void onSearchCompleted_whenMemberIdIsNull_neverCallsUpsert() {
		// when
		listener.onSearchCompleted(new SearchCompletedEvent(null, "치킨", 1, 1));

		// then
		then(memberSearchHistoryRepository).should(never()).upsertSearchHistory(anyLong(), anyString());
	}
}
