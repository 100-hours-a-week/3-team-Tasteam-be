package com.tasteam.domain.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.favorite.entity.SubgroupFavoriteRestaurant;

@RepositoryJpaTest
@DisplayName("SubgroupFavoriteRestaurantRepository 테스트")
class SubgroupFavoriteRestaurantRepositoryTest {

	@Autowired
	private SubgroupFavoriteRestaurantRepository repository;

	@Test
	@DisplayName("하위그룹 찜을 저장하면 조회할 수 있다")
	void saveAndFind() {
		SubgroupFavoriteRestaurant favorite = repository.save(SubgroupFavoriteRestaurant.create(1L, 10L, 100L));

		assertThat(favorite.getId()).isNotNull();
		assertThat(repository.findBySubgroupIdAndRestaurantId(10L, 100L)).isPresent();
	}

	@Test
	@DisplayName("(subgroup_id, restaurant_id)는 유니크 제약조건을 가진다")
	void duplicateSubgroupRestaurant_throwsException() {
		repository.saveAndFlush(SubgroupFavoriteRestaurant.create(1L, 11L, 101L));

		assertThatThrownBy(() -> repository.saveAndFlush(SubgroupFavoriteRestaurant.create(2L, 11L, 101L)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("(restaurant_id, member_id)는 유니크 제약조건을 가진다")
	void duplicateRestaurantMember_throwsException() {
		repository.saveAndFlush(SubgroupFavoriteRestaurant.create(3L, 12L, 102L));

		assertThatThrownBy(() -> repository.saveAndFlush(SubgroupFavoriteRestaurant.create(3L, 13L, 102L)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("deleteBySubgroupIdAndRestaurantId는 대상 데이터만 삭제한다")
	void deleteBySubgroupIdAndRestaurantId_deletesTargetOnly() {
		repository.saveAndFlush(SubgroupFavoriteRestaurant.create(4L, 14L, 104L));
		repository.saveAndFlush(SubgroupFavoriteRestaurant.create(5L, 15L, 105L));

		repository.deleteBySubgroupIdAndRestaurantId(14L, 104L);

		assertThat(repository.findBySubgroupIdAndRestaurantId(14L, 104L)).isEmpty();
		assertThat(repository.findBySubgroupIdAndRestaurantId(15L, 105L)).isPresent();
	}
}
