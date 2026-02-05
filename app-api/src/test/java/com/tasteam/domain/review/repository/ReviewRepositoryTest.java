package com.tasteam.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.fixture.MemberFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("ReviewRepository 테스트")
class ReviewRepositoryTest {

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	private Restaurant saveRestaurant(String name) {
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
		Point point = gf.createPoint(new Coordinate(126.978, 37.5665));
		return restaurantRepository.save(Restaurant.create(name, "서울시 강남구", point, "02-1234-5678"));
	}

	private Review saveReview(Restaurant restaurant, Member member, boolean isRecommended) {
		return reviewRepository.save(
			Review.create(restaurant, member, 1L, null, "테스트 리뷰", isRecommended));
	}

	@Test
	@DisplayName("리뷰를 저장하면 기본 매핑과 연관관계가 정상이다")
	void saveAndFind() {
		Restaurant restaurant = saveRestaurant("테스트식당");
		Member member = memberRepository.save(MemberFixture.create());

		Review saved = saveReview(restaurant, member, true);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getRestaurant().getId()).isEqualTo(restaurant.getId());
		assertThat(saved.getMember().getId()).isEqualTo(member.getId());
		assertThat(saved.isRecommended()).isTrue();
		assertThat(saved.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("countByRestaurantIdAndIsRecommendedTrueAndDeletedAtIsNull - recommended별 카운트가 분리된다")
	void countByRestaurantIdAndIsRecommendedTrueAndDeletedAtIsNull_separatesRecommended() {
		Restaurant restaurant = saveRestaurant("카운트테스트식당");
		Member member = memberRepository.save(MemberFixture.create());
		saveReview(restaurant, member, true);
		saveReview(restaurant, member, true);
		saveReview(restaurant, member, false);
		Review deleted = saveReview(restaurant, member, true);
		deleted.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		long trueCount = reviewRepository.countByRestaurantIdAndIsRecommendedTrueAndDeletedAtIsNull(restaurant.getId());
		long falseCount = reviewRepository
			.countByRestaurantIdAndIsRecommendedFalseAndDeletedAtIsNull(restaurant.getId());

		assertThat(trueCount).isEqualTo(2);
		assertThat(falseCount).isEqualTo(1);
	}

	@Test
	@DisplayName("findByIdAndDeletedAtIsNull - 소프트 삭제된 리뷰는 조회되지 않는다")
	void findByIdAndDeletedAtIsNull_excludesDeleted() {
		Restaurant restaurant = saveRestaurant("삭제테스트식당");
		Member member = memberRepository.save(MemberFixture.create());
		Review review = saveReview(restaurant, member, true);
		review.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		var result = reviewRepository.findByIdAndDeletedAtIsNull(review.getId());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findByRestaurantIdAndDeletedAtIsNull - 리스트 조회에서 삭제된 리뷰가 제외된다")
	void findByRestaurantIdAndDeletedAtIsNull_excludesDeleted() {
		Restaurant restaurant = saveRestaurant("리스트테스트식당");
		Member member = memberRepository.save(MemberFixture.create());
		saveReview(restaurant, member, true);
		Review deleted = saveReview(restaurant, member, false);
		deleted.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		List<Review> results = reviewRepository.findByRestaurantIdAndDeletedAtIsNull(restaurant.getId());

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getDeletedAt()).isNull();
	}
}
