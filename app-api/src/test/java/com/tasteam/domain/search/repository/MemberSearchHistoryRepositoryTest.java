package com.tasteam.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.fixture.MemberSearchHistoryFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("MemberSearchHistoryRepository 테스트")
class MemberSearchHistoryRepositoryTest {

	@Autowired
	private MemberSearchHistoryRepository memberSearchHistoryRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("검색 히스토리 저장 및 조회")
	void saveAndFind() {
		MemberSearchHistory history = MemberSearchHistoryFixture.create();

		MemberSearchHistory saved = memberSearchHistoryRepository.save(history);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getMemberId()).isEqualTo(MemberSearchHistoryFixture.DEFAULT_MEMBER_ID);
		assertThat(saved.getKeyword()).isEqualTo(MemberSearchHistoryFixture.DEFAULT_KEYWORD);
		assertThat(saved.getCount()).isEqualTo(1L);
		assertThat(saved.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("findByMemberIdAndKeywordAndDeletedAtIsNull - 삭제된 검색어는 조회되지 않음")
	void findByMemberIdAndKeywordAndDeletedAtIsNull_excludesDeleted() {
		MemberSearchHistory history = MemberSearchHistoryFixture.create();
		MemberSearchHistory saved = memberSearchHistoryRepository.save(history);
		saved.delete();
		entityManager.flush();
		entityManager.clear();

		var result = memberSearchHistoryRepository
			.findByMemberIdAndKeywordAndDeletedAtIsNull(
				saved.getMemberId(),
				saved.getKeyword());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findByIdAndMemberIdAndDeletedAtIsNull - 본인의 검색어만 조회")
	void findByIdAndMemberIdAndDeletedAtIsNull_onlyOwnHistory() {
		MemberSearchHistory history1 = memberSearchHistoryRepository.save(
			MemberSearchHistoryFixture.create(1L, "검색어1"));
		memberSearchHistoryRepository.save(
			MemberSearchHistoryFixture.create(2L, "검색어2"));

		var result = memberSearchHistoryRepository
			.findByIdAndMemberIdAndDeletedAtIsNull(history1.getId(), 2L);

		assertThat(result).isEmpty();
	}
}
