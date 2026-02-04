package com.tasteam.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.entity.Keyword;
import com.tasteam.domain.review.entity.KeywordType;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.entity.ReviewKeyword;
import com.tasteam.domain.review.repository.projection.ReviewKeywordProjection;
import com.tasteam.fixture.MemberFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("ReviewKeywordRepository 테스트")
class ReviewKeywordRepositoryTest {

	@Autowired
	private ReviewKeywordRepository reviewKeywordRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	private Restaurant saveRestaurant() {
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
		Point point = gf.createPoint(new Coordinate(126.978, 37.5665));
		return restaurantRepository.save(Restaurant.create("키워드테스트식당", "서울시 강남구", point, "02-1234-5678"));
	}

	private Review saveReview(Restaurant restaurant, Member member, String content) {
		return reviewRepository.save(
			Review.create(restaurant, member, 1L, null, content, true));
	}

	@Test
	@DisplayName("findReviewKeywords - 복수 리뷰의 키워드를 프로젝션으로 조회한다")
	void findReviewKeywords_returnsKeywordsForMultipleReviews() {
		Restaurant restaurant = saveRestaurant();
		Member member = memberRepository.save(MemberFixture.create());
		Review review1 = saveReview(restaurant, member, "리뷰1");
		Review review2 = saveReview(restaurant, member, "리뷰2");
		Keyword keyword1 = keywordRepository.save(Keyword.create(KeywordType.POSITIVE_ASPECT, "맛있어요"));
		Keyword keyword2 = keywordRepository.save(Keyword.create(KeywordType.POSITIVE_ASPECT, "깔끔해요"));
		reviewKeywordRepository.save(ReviewKeyword.create(review1, keyword1));
		reviewKeywordRepository.save(ReviewKeyword.create(review2, keyword2));
		entityManager.flush();
		entityManager.clear();

		List<ReviewKeywordProjection> results = reviewKeywordRepository.findReviewKeywords(
			List.of(review1.getId(), review2.getId()));

		assertThat(results).hasSize(2);
		assertThat(results).extracting(ReviewKeywordProjection::getKeywordName)
			.containsExactlyInAnyOrder("맛있어요", "깔끔해요");
	}

	@Test
	@DisplayName("deleteByReview_Id - 특정 리뷰의 키워드만 삭제되고 다른 리뷰는 영향받지 않는다")
	void deleteByReview_Id_deletesOnlyTargetReview() {
		Restaurant restaurant = saveRestaurant();
		Member member = memberRepository.save(MemberFixture.create());
		Review review1 = saveReview(restaurant, member, "삭제대상");
		Review review2 = saveReview(restaurant, member, "유지대상");
		Keyword keyword = keywordRepository.save(Keyword.create(KeywordType.POSITIVE_ASPECT, "공유키워드"));
		reviewKeywordRepository.save(ReviewKeyword.create(review1, keyword));
		reviewKeywordRepository.save(ReviewKeyword.create(review2, keyword));
		entityManager.flush();
		entityManager.clear();

		reviewKeywordRepository.deleteByReview_Id(review1.getId());
		entityManager.flush();
		entityManager.clear();

		List<ReviewKeywordProjection> results = reviewKeywordRepository.findReviewKeywords(
			List.of(review1.getId(), review2.getId()));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getReviewId()).isEqualTo(review2.getId());
	}

	@Test
	@DisplayName("save - 동일한 (review_id, keyword_id)를 저장하면 제약조건 예외가 발생한다")
	void save_duplicateReviewKeyword_throwsDataIntegrityViolationException() {
		Restaurant restaurant = saveRestaurant();
		Member member = memberRepository.save(MemberFixture.create());
		Review review = saveReview(restaurant, member, "중복테스트");
		Keyword keyword = keywordRepository.save(Keyword.create(KeywordType.POSITIVE_ASPECT, "중복키워드"));
		reviewKeywordRepository.save(ReviewKeyword.create(review, keyword));
		reviewKeywordRepository.save(ReviewKeyword.create(review, keyword));

		assertThatThrownBy(() -> entityManager.flush())
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
