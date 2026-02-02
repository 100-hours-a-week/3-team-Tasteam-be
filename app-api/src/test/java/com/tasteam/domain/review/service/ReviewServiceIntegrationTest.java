package com.tasteam.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.dto.request.ReviewCreateRequest;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.domain.review.dto.response.ReviewDetailResponse;
import com.tasteam.domain.review.entity.Keyword;
import com.tasteam.domain.review.entity.KeywordType;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.repository.KeywordRepository;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.MemberFixture;

@ServiceIntegrationTest
@Transactional
class ReviewServiceIntegrationTest {

	@Autowired
	private ReviewService reviewService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	private Member member;
	private Restaurant restaurant;
	private Group group;
	private Keyword keyword;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(MemberFixture.create());
		restaurant = restaurantRepository.save(createRestaurant());
		group = groupRepository.save(GroupFixture.create());
		keyword = keywordRepository.save(Keyword.create(KeywordType.POSITIVE_ASPECT, "맛있어요"));
	}

	@Nested
	@DisplayName("리뷰 생성")
	class CreateReview {

		@Test
		@DisplayName("이미지 없이 리뷰를 생성한다")
		void createReviewWithoutImages() {
			ReviewCreateRequest request = new ReviewCreateRequest(
				group.getId(),
				null,
				"맛있는 음식점입니다",
				true,
				List.of(keyword.getId()),
				null);

			ReviewCreateResponse response = reviewService.createReview(
				member.getId(),
				restaurant.getId(),
				request);

			assertThat(response.id()).isNotNull();
			assertThat(response.createdAt()).isNotNull();
		}

		@Test
		@DisplayName("이미지와 함께 리뷰를 생성하면 이미지가 ACTIVE 상태로 변경된다")
		void createReviewWithImages() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(
				Image.create(FilePurpose.REVIEW_IMAGE, "review.png", 1024L, "image/png",
					"reviews/review.png", fileUuid));

			ReviewCreateRequest request = new ReviewCreateRequest(
				group.getId(),
				null,
				"사진과 함께 리뷰합니다",
				true,
				List.of(keyword.getId()),
				List.of(fileUuid));

			ReviewCreateResponse response = reviewService.createReview(
				member.getId(),
				restaurant.getId(),
				request);

			assertThat(response.id()).isNotNull();

			Image updatedImage = imageRepository.findByFileUuid(fileUuid).orElseThrow();
			assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.REVIEW, List.of(response.id()));
			assertThat(domainImages).hasSize(1);
		}
	}

	@Nested
	@DisplayName("리뷰 상세 조회")
	class GetReviewDetail {

		@Test
		@DisplayName("리뷰 상세를 조회하면 키워드와 이미지가 포함된다")
		void getReviewDetailWithKeywordsAndImages() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(
				Image.create(FilePurpose.REVIEW_IMAGE, "review.png", 1024L, "image/png",
					"reviews/review.png", fileUuid));

			ReviewCreateRequest request = new ReviewCreateRequest(
				group.getId(),
				null,
				"상세 조회 테스트",
				true,
				List.of(keyword.getId()),
				List.of(fileUuid));

			ReviewCreateResponse created = reviewService.createReview(
				member.getId(),
				restaurant.getId(),
				request);

			ReviewDetailResponse detail = reviewService.getReviewDetail(created.id());

			assertThat(detail.id()).isEqualTo(created.id());
			assertThat(detail.content()).isEqualTo("상세 조회 테스트");
			assertThat(detail.keywords()).contains("맛있어요");
			assertThat(detail.images()).hasSize(1);
		}
	}

	@Nested
	@DisplayName("리뷰 삭제")
	class DeleteReview {

		@Test
		@DisplayName("리뷰를 삭제하면 soft delete 처리된다")
		void deleteReview() {
			ReviewCreateRequest request = new ReviewCreateRequest(
				group.getId(),
				null,
				"삭제될 리뷰",
				true,
				List.of(keyword.getId()),
				null);

			ReviewCreateResponse created = reviewService.createReview(
				member.getId(),
				restaurant.getId(),
				request);

			reviewService.deleteReview(member.getId(), created.id());

			Review deletedReview = reviewRepository.findById(created.id()).orElseThrow();
			assertThat(deletedReview.getDeletedAt()).isNotNull();
		}
	}

	private Restaurant createRestaurant() {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			"테스트 음식점",
			"서울시 강남구 테헤란로 123",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)));
	}
}
