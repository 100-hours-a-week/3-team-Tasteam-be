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
import org.springframework.context.annotation.Import;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.dto.ReviewDetailQueryDto;
import com.tasteam.domain.review.dto.ReviewQueryDto;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.repository.impl.ReviewQueryRepositoryImpl;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.MemberFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@Import(ReviewQueryRepositoryImpl.class)
@DisplayName("ReviewQueryRepository 테스트")
class ReviewQueryRepositoryTest {

	@Autowired
	private ReviewQueryRepository reviewQueryRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private EntityManager entityManager;

	private Restaurant saveRestaurant(String name) {
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
		Point point = gf.createPoint(new Coordinate(126.978, 37.5665));
		return restaurantRepository.save(Restaurant.create(name, "서울시 강남구", point, "02-1234-5678"));
	}

	private Review saveReview(Restaurant restaurant, Member member, Long groupId, String content) {
		return reviewRepository.save(
			Review.create(restaurant, member, groupId, null, content, true));
	}

	@Test
	@DisplayName("findRestaurantReviews - 삭제된 리뷰는 제외되고 createdAt desc로 정렬된다")
	void findRestaurantReviews_excludesDeletedReview() {
		Group group = groupRepository.save(GroupFixture.create("리뷰조회그룹", "서울시 강남구"));
		Restaurant restaurant = saveRestaurant("리뷰조회식당");
		Member member = memberRepository.save(MemberFixture.create());
		saveReview(restaurant, member, group.getId(), "활성 리뷰");
		Review deleted = saveReview(restaurant, member, group.getId(), "삭제된 리뷰");
		deleted.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		List<ReviewQueryDto> results = reviewQueryRepository.findRestaurantReviews(
			restaurant.getId(), null, 10);

		assertThat(results).hasSize(1);
		ReviewQueryDto result = results.get(0);
		assertThat(result.content()).isEqualTo("활성 리뷰");
		assertThat(result.groupName()).isEqualTo("리뷰조회그룹");
	}

	@Test
	@DisplayName("findGroupReviews - 특정 groupId의 리뷰만 반환된다")
	void findGroupReviews_returnsOnlyTargetGroup() {
		Group group10 = groupRepository.save(GroupFixture.create("그룹10", "서울시 강남구"));
		Group group20 = groupRepository.save(GroupFixture.create("그룹20", "서울시 서초구"));
		Restaurant restaurant = saveRestaurant("그룹리뷰식당");
		Member member = memberRepository.save(MemberFixture.create());
		saveReview(restaurant, member, group10.getId(), "그룹10 리뷰");
		saveReview(restaurant, member, group20.getId(), "그룹20 리뷰");
		entityManager.flush();
		entityManager.clear();

		List<ReviewQueryDto> results = reviewQueryRepository.findGroupReviews(group10.getId(), null, 10);

		assertThat(results).hasSize(1);
		ReviewQueryDto result = results.get(0);
		assertThat(result.content()).isEqualTo("그룹10 리뷰");
		assertThat(result.groupName()).isEqualTo("그룹10");
	}

	@Test
	@DisplayName("findMemberReviews - 삭제된 레스토랑의 리뷰는 제외된다")
	void findMemberReviews_excludesDeletedRestaurant() {
		Group group = groupRepository.save(GroupFixture.create());
		Restaurant activeRestaurant = saveRestaurant("활성식당");
		Restaurant deletedRestaurant = saveRestaurant("삭제식당");
		Member member = memberRepository.save(MemberFixture.create());
		saveReview(activeRestaurant, member, group.getId(), "활성식당 리뷰");
		saveReview(deletedRestaurant, member, group.getId(), "삭제식당 리뷰");
		deletedRestaurant.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		List<com.tasteam.domain.review.dto.ReviewMemberQueryDto> results = reviewQueryRepository
			.findMemberReviews(member.getId(), null, 10);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).content()).isEqualTo("활성식당 리뷰");
	}

	@Test
	@DisplayName("findReviewDetail - 단일 리뷰 상세 조회에서 restaurant와 member 정보가 포함된다")
	void findReviewDetail_success() {
		Group group = groupRepository.save(GroupFixture.create());
		Restaurant restaurant = saveRestaurant("상세조회식당");
		Member member = memberRepository.save(MemberFixture.create());
		Review review = saveReview(restaurant, member, group.getId(), "상세 리뷰 내용");
		entityManager.flush();
		entityManager.clear();

		ReviewDetailQueryDto result = reviewQueryRepository.findReviewDetail(review.getId());

		assertThat(result).isNotNull();
		assertThat(result.restaurantName()).isEqualTo("상세조회식당");
		assertThat(result.memberNickname()).isEqualTo(MemberFixture.DEFAULT_NICKNAME);
		assertThat(result.content()).isEqualTo("상세 리뷰 내용");
	}

	@Test
	@DisplayName("findReviewDetail - 소프트 삭제된 리뷰는 null을 반환한다")
	void findReviewDetail_deletedReview_returnsNull() {
		Group group = groupRepository.save(GroupFixture.create());
		Restaurant restaurant = saveRestaurant("삭제상세식당");
		Member member = memberRepository.save(MemberFixture.create());
		Review review = saveReview(restaurant, member, group.getId(), "삭제될 리뷰");
		review.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		ReviewDetailQueryDto result = reviewQueryRepository.findReviewDetail(review.getId());

		assertThat(result).isNull();
	}
}
