package com.tasteam.domain.favorite.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("즐겨찾기 엔티티")
class MemberFavoriteRestaurantTest {

	@Nested
	@DisplayName("즐겨찾기 생성·삭제·복귀")
	class FavoriteLifecycle {

		@Test
		@DisplayName("즐겨찾기를 생성하면 deletedAt이 null이다")
		void create_setsDeletedAtToNull() {
			MemberFavoriteRestaurant favorite = MemberFavoriteRestaurant.create(1L, 2L);

			assertThat(favorite.getDeletedAt()).isNull();
			assertThat(favorite.getMemberId()).isEqualTo(1L);
			assertThat(favorite.getRestaurantId()).isEqualTo(2L);
		}

		@Test
		@DisplayName("즐겨찾기를 삭제하면 deletedAt이 설정된다")
		void delete_setsDeletedAt() {
			MemberFavoriteRestaurant favorite = MemberFavoriteRestaurant.create(1L, 2L);

			favorite.delete();

			assertThat(favorite.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("삭제된 즐겨찾기를 복귀 처리하면 deletedAt이 초기화된다")
		void restore_clearsDeletedAt() {
			MemberFavoriteRestaurant favorite = MemberFavoriteRestaurant.create(1L, 2L);
			favorite.delete();

			favorite.restore();

			assertThat(favorite.getDeletedAt()).isNull();
		}
	}
}
