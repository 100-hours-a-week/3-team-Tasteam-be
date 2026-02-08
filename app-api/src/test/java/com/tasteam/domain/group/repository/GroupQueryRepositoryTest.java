package com.tasteam.domain.group.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.impl.GroupQueryRepositoryImpl;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.fixture.GroupFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@Import(GroupQueryRepositoryImpl.class)
@DisplayName("GroupQueryRepository 테스트")
class GroupQueryRepositoryTest {

	@Autowired
	private GroupQueryRepository groupQueryRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("searchByKeyword - 그룹명으로 검색")
	void searchByKeyword_byName() {
		groupRepository.save(GroupFixture.create("맛집탐방모임", "서울시 강남구"));
		groupRepository.save(GroupFixture.create("카페투어모임", "서울시 강남구"));
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"맛집",
			GroupStatus.ACTIVE,
			10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getName()).contains("맛집");
	}

	@Test
	@DisplayName("searchByKeyword - 주소로 검색")
	void searchByKeyword_byAddress() {
		groupRepository.save(GroupFixture.create("강남모임", "서울시 강남구"));
		groupRepository.save(GroupFixture.create("홍대모임", "서울시 마포구"));
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"마포",
			GroupStatus.ACTIVE,
			10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getAddress()).contains("마포");
	}

	@Test
	@DisplayName("searchByKeyword - 대소문자 구분 없이 검색")
	void searchByKeyword_caseInsensitive() {
		groupRepository.save(GroupFixture.create("Cafe Lovers", "서울시 강남구"));
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"cafe",
			GroupStatus.ACTIVE,
			10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getName()).containsIgnoringCase("cafe");
	}

	@Test
	@DisplayName("searchByKeyword - ACTIVE 상태만 조회")
	void searchByKeyword_onlyActiveGroups() {
		groupRepository.save(GroupFixture.createWithStatus(GroupStatus.ACTIVE));
		groupRepository.save(GroupFixture.createWithStatus(GroupStatus.INACTIVE));
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"테스트",
			GroupStatus.ACTIVE,
			10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getStatus()).isEqualTo(GroupStatus.ACTIVE);
	}

	@Test
	@DisplayName("searchByKeyword - 삭제된 그룹 제외")
	void searchByKeyword_excludesDeleted() {
		Group active = groupRepository.save(GroupFixture.create("활성그룹", "서울시"));
		Group deleted = groupRepository.save(GroupFixture.create("삭제그룹", "서울시"));
		deleted.delete(java.time.Instant.now());
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"그룹",
			GroupStatus.ACTIVE,
			10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getName()).isEqualTo("활성그룹");
	}

	@Test
	@DisplayName("searchByKeyword - updated_at 내림차순 정렬")
	void searchByKeyword_orderedByUpdatedAtDesc() {
		Group group1 = groupRepository.save(GroupFixture.create("그룹1", "서울시"));
		Group group2 = groupRepository.save(GroupFixture.create("그룹2", "서울시"));
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"그룹",
			GroupStatus.ACTIVE,
			10);

		assertThat(results).hasSize(2);
		assertThat(results.get(0).getUpdatedAt()).isAfterOrEqualTo(results.get(1).getUpdatedAt());
	}

	@Test
	@DisplayName("searchByKeyword - pageSize 제한")
	void searchByKeyword_limitByPageSize() {
		for (int i = 1; i <= 5; i++) {
			groupRepository.save(GroupFixture.create("그룹" + i, "서울시"));
		}
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"그룹",
			GroupStatus.ACTIVE,
			3);

		assertThat(results).hasSize(3);
	}

	@Test
	@DisplayName("searchByKeyword - 그룹명 또는 주소 중 하나라도 일치하면 조회")
	void searchByKeyword_matchesNameOrAddress() {
		groupRepository.save(GroupFixture.create("카페모임", "서울시 강남구"));
		groupRepository.save(GroupFixture.create("강남맛집투어", "서울시 마포구"));
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"강남",
			GroupStatus.ACTIVE,
			10);

		assertThat(results).hasSize(2);
	}

	@Test
	@DisplayName("searchByKeyword - location 필드도 정상 조회")
	void searchByKeyword_includesLocationField() {
		groupRepository.save(GroupFixture.create("위치테스트그룹", "서울시"));
		entityManager.flush();
		entityManager.clear();

		List<Group> results = groupQueryRepository.searchByKeyword(
			"위치",
			GroupStatus.ACTIVE,
			10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getLocation()).isNotNull();
		assertThat(results.get(0).getLocation().getX()).isNotNull();
		assertThat(results.get(0).getLocation().getY()).isNotNull();
	}
}
