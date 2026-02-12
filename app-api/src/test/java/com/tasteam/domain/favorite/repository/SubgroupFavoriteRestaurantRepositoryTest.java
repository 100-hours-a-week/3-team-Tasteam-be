package com.tasteam.domain.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
	@DisplayName("삭제된 데이터가 있으면 동일 찜을 다시 저장할 수 있다")
	void allowInsertWhenDeleted() {
		repository.saveAndFlush(SubgroupFavoriteRestaurant.create(3L, 12L, 102L));
		SubgroupFavoriteRestaurant deleted = repository.findByMemberIdAndSubgroupIdAndRestaurantId(3L, 12L, 102L)
			.orElseThrow();
		deleted.delete();
		repository.flush();

		SubgroupFavoriteRestaurant inserted = repository.saveAndFlush(SubgroupFavoriteRestaurant.create(3L, 12L,
			102L));
		assertThat(inserted.getId()).isNotNull();
	}

	@Test
	@DisplayName("findByMemberIdAndSubgroupIdAndRestaurantIdAndDeletedAtIsNull은 삭제된 행을 제외한다")
	void findActive_excludesDeleted() {
		repository.saveAndFlush(SubgroupFavoriteRestaurant.create(4L, 14L, 104L));
		SubgroupFavoriteRestaurant deleted = repository.findByMemberIdAndSubgroupIdAndRestaurantId(4L, 14L, 104L)
			.orElseThrow();
		deleted.delete();
		repository.flush();

		assertThat(repository.findByMemberIdAndSubgroupIdAndRestaurantIdAndDeletedAtIsNull(4L, 14L, 104L)).isEmpty();
	}
}
