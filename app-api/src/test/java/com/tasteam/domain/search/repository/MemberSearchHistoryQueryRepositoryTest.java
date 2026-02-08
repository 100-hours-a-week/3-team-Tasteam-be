package com.tasteam.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.domain.search.repository.impl.MemberSearchHistoryQueryRepositoryImpl;
import com.tasteam.fixture.MemberSearchHistoryFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@Import(MemberSearchHistoryQueryRepositoryImpl.class)
@DisplayName("MemberSearchHistoryQueryRepository 테스트")
class MemberSearchHistoryQueryRepositoryTest {

	@Autowired
	private MemberSearchHistoryQueryRepository memberSearchHistoryQueryRepository;

	@Autowired
	private MemberSearchHistoryRepository memberSearchHistoryRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("findRecentSearches - 최근 검색어 조회 (updated_at 내림차순)")
	void findRecentSearches_orderedByUpdatedAtDesc() {
		Long memberId = 1L;
		memberSearchHistoryRepository.save(MemberSearchHistoryFixture.create(memberId, "검색어1"));
		memberSearchHistoryRepository.save(MemberSearchHistoryFixture.create(memberId, "검색어2"));
		memberSearchHistoryRepository.save(MemberSearchHistoryFixture.create(memberId, "검색어3"));
		entityManager.flush();
		entityManager.clear();

		List<MemberSearchHistory> results = memberSearchHistoryQueryRepository.findRecentSearches(
			memberId,
			null,
			10);

		assertThat(results).hasSize(3);
		assertThat(results.get(0).getUpdatedAt()).isAfterOrEqualTo(results.get(1).getUpdatedAt());
		assertThat(results.get(1).getUpdatedAt()).isAfterOrEqualTo(results.get(2).getUpdatedAt());
	}

	@Test
	@DisplayName("findRecentSearches - 삭제된 검색어는 제외")
	void findRecentSearches_excludesDeleted() {
		Long memberId = 1L;
		memberSearchHistoryRepository.save(MemberSearchHistoryFixture.create(memberId, "검색어1"));
		MemberSearchHistory deleted = memberSearchHistoryRepository.save(
			MemberSearchHistoryFixture.create(memberId, "삭제된검색어"));
		deleted.delete();
		entityManager.flush();
		entityManager.clear();

		List<MemberSearchHistory> results = memberSearchHistoryQueryRepository.findRecentSearches(
			memberId,
			null,
			10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getKeyword()).isEqualTo("검색어1");
	}

	@Test
	@DisplayName("findRecentSearches - 다른 회원의 검색어는 제외")
	void findRecentSearches_onlyOwnSearches() {
		memberSearchHistoryRepository.save(MemberSearchHistoryFixture.create(1L, "회원1검색어"));
		memberSearchHistoryRepository.save(MemberSearchHistoryFixture.create(2L, "회원2검색어"));

		List<MemberSearchHistory> results = memberSearchHistoryQueryRepository.findRecentSearches(
			1L,
			null,
			10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getKeyword()).isEqualTo("회원1검색어");
	}

	@Test
	@DisplayName("findRecentSearches - pageSize 제한")
	void findRecentSearches_limitByPageSize() {
		Long memberId = 1L;
		for (int i = 1; i <= 5; i++) {
			memberSearchHistoryRepository.save(
				MemberSearchHistoryFixture.create(memberId, "검색어" + i));
		}
		entityManager.flush();
		entityManager.clear();

		List<MemberSearchHistory> results = memberSearchHistoryQueryRepository.findRecentSearches(
			memberId,
			null,
			3);

		assertThat(results).hasSize(3);
	}
}
