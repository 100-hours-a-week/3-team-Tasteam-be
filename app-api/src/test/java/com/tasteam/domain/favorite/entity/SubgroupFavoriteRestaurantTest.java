package com.tasteam.domain.favorite.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("하위그룹 찜 엔티티")
class SubgroupFavoriteRestaurantTest {

	@Test
	@DisplayName("하위그룹 찜을 생성하면 값이 정상 매핑된다")
	void create_mapsFields() {
		SubgroupFavoriteRestaurant favorite = SubgroupFavoriteRestaurant.create(1L, 2L, 3L);

		assertThat(favorite.getId()).isNull();
		assertThat(favorite.getMemberId()).isEqualTo(1L);
		assertThat(favorite.getSubgroupId()).isEqualTo(2L);
		assertThat(favorite.getRestaurantId()).isEqualTo(3L);
	}
}
