package com.tasteam.domain.search.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("검색 히스토리 엔티티")
class MemberSearchHistoryTest {

	@Nested
	@DisplayName("검색 히스토리 생성")
	class CreateSearchHistory {

		@Test
		@DisplayName("검색 히스토리를 생성하면 count가 1이다")
		void create_setsCountToOne() {
			MemberSearchHistory history = MemberSearchHistory.create(1L, "테스트키워드");

			assertThat(history.getCount()).isEqualTo(1L);
		}

		@Test
		@DisplayName("검색 히스토리를 생성하면 deletedAt이 null이다")
		void create_setsDeletedAtToNull() {
			MemberSearchHistory history = MemberSearchHistory.create(1L, "테스트키워드");

			assertThat(history.getDeletedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("검색 히스토리 활동")
	class SearchHistoryActivity {

		@Test
		@DisplayName("검색 히스토리의 count를 증가시키면 1에서 2로 된다")
		void incrementCount_incrementsCount() {
			MemberSearchHistory history = MemberSearchHistory.create(1L, "테스트키워드");

			history.incrementCount();

			assertThat(history.getCount()).isEqualTo(2L);
		}

		@Test
		@DisplayName("검색 히스토리를 삭제하면 deletedAt이 설정된다")
		void delete_setsDeletedAt() {
			MemberSearchHistory history = MemberSearchHistory.create(1L, "테스트키워드");

			history.delete();

			assertThat(history.getDeletedAt()).isNotNull();
		}
	}
}
