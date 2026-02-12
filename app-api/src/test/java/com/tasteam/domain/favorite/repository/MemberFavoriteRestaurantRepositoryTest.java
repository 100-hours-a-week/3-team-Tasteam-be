package com.tasteam.domain.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.favorite.entity.MemberFavoriteRestaurant;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("MemberFavoriteRestaurantRepository 테스트")
class MemberFavoriteRestaurantRepositoryTest {

	@Autowired
	private MemberFavoriteRestaurantRepository memberFavoriteRestaurantRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("즐겨찾기를 저장하면 기본 매핑이 정상이다")
	void saveAndFind() {
		MemberFavoriteRestaurant favorite = MemberFavoriteRestaurant.create(1L, 100L);

		MemberFavoriteRestaurant saved = memberFavoriteRestaurantRepository.save(favorite);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getMemberId()).isEqualTo(1L);
		assertThat(saved.getRestaurantId()).isEqualTo(100L);
		assertThat(saved.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("findByMemberIdAndRestaurantIdAndDeletedAtIsNull - 삭제된 즐겨찾기는 조회되지 않는다")
	void findByMemberIdAndRestaurantIdAndDeletedAtIsNull_excludesDeleted() {
		MemberFavoriteRestaurant favorite = memberFavoriteRestaurantRepository.save(
			MemberFavoriteRestaurant.create(2L, 200L));
		favorite.delete();
		entityManager.flush();
		entityManager.clear();

		var result = memberFavoriteRestaurantRepository
			.findByMemberIdAndRestaurantIdAndDeletedAtIsNull(2L, 200L);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findByMemberIdAndRestaurantId - deletedAt 필터 없이 삭제된 즐겨찾기도 포함된다")
	void findByMemberIdAndRestaurantId_includesDeleted() {
		MemberFavoriteRestaurant favorite = memberFavoriteRestaurantRepository.save(
			MemberFavoriteRestaurant.create(3L, 300L));
		favorite.delete();
		entityManager.flush();
		entityManager.clear();

		var result = memberFavoriteRestaurantRepository.findByMemberIdAndRestaurantId(3L, 300L);

		assertThat(result).isPresent();
		assertThat(result.get().getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("save - 동일한 (member_id, restaurant_id)를 저장하면 제약조건 예외가 발생한다")
	void save_duplicateFavorite_throwsDataIntegrityViolationException() {
		memberFavoriteRestaurantRepository.saveAndFlush(MemberFavoriteRestaurant.create(4L, 400L));

		assertThatThrownBy(() -> memberFavoriteRestaurantRepository.saveAndFlush(
			MemberFavoriteRestaurant.create(4L, 400L)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
