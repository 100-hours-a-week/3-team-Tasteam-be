package com.tasteam.domain.review.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.fixture.MemberFixture;

@UnitTest
@DisplayName("리뷰-키워드 매핑 엔티티")
class ReviewKeywordTest {

	private Review createReview() {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		Restaurant restaurant = Restaurant.create(
			"테스트음식점", "서울시 강남구 테헤란로 1",
			geometryFactory.createPoint(new Coordinate(126.978, 37.5665)), "02-1234-5678");
		Member member = MemberFixture.create();
		return Review.create(restaurant, member, 1L, null, "테스트 리뷰", true);
	}

	private Keyword createKeyword() {
		return Keyword.create(KeywordType.POSITIVE_ASPECT, "테스트키워드");
	}

	@Nested
	@DisplayName("리뷰-키워드 매핑 생성")
	class CreateReviewKeyword {

		@Test
		@DisplayName("유효한 리뷰와 키워드로 매핑을 생성한다")
		void create_validParams_createsReviewKeyword() {
			Review review = createReview();
			Keyword keyword = createKeyword();

			ReviewKeyword reviewKeyword = ReviewKeyword.create(review, keyword);

			assertThat(reviewKeyword.getReview()).isEqualTo(review);
			assertThat(reviewKeyword.getKeyword()).isEqualTo(keyword);
		}

		@Test
		@DisplayName("리뷰가 null이면 매핑 생성에 실패한다")
		void create_nullReview_throwsIllegalArgumentException() {
			Keyword keyword = createKeyword();

			assertThatThrownBy(() -> ReviewKeyword.create(null, keyword))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("키워드가 null이면 매핑 생성에 실패한다")
		void create_nullKeyword_throwsIllegalArgumentException() {
			Review review = createReview();

			assertThatThrownBy(() -> ReviewKeyword.create(review, null))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}
}
