package com.tasteam.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.domain.review.entity.Keyword;
import com.tasteam.domain.review.entity.KeywordType;
import com.tasteam.domain.review.repository.KeywordRepository;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.fixture.ReviewRequestFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

@ServiceIntegrationTest
@Transactional
class ReviewImageDeletionIntegrationTest {

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
		keyword = keywordRepository.save(Keyword.create(KeywordType.POSITIVE_ASPECT, "깔끔해요"));
	}

	@Nested
	@DisplayName("리뷰 삭제 시 이미지 연관 정리")
	class DeleteReviewImages {

		@Test
		@DisplayName("이미지가 있는 리뷰를 삭제하면 연관된 DomainImage가 제거된다")
		void deleteReview_withImage_removesDomainImage() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(ImageFixture.create(FilePurpose.REVIEW_IMAGE, "reviews/delete-target.png", fileUuid,
				"delete-target.png"));

			var request = ReviewRequestFixture.createRequest(group.getId(), List.of(keyword.getId()),
				List.of(fileUuid));

			ReviewCreateResponse created = reviewService.createReview(
				member.getId(),
				restaurant.getId(),
				request);

			List<DomainImage> before = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.REVIEW, List.of(created.id()));
			assertThat(before).hasSize(1);

			reviewService.deleteReview(member.getId(), created.id());

			List<DomainImage> after = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.REVIEW, List.of(created.id()));
			assertThat(after).isEmpty();
		}

		@Test
		@DisplayName("이미지가 여러 개인 리뷰를 삭제하면 모든 DomainImage가 제거된다")
		void deleteReview_withMultipleImages_removesAllDomainImages() {
			UUID fileUuid1 = UUID.randomUUID();
			UUID fileUuid2 = UUID.randomUUID();
			imageRepository
				.save(ImageFixture.create(FilePurpose.REVIEW_IMAGE, "reviews/multi-1.png", fileUuid1, "multi-1.png"));
			imageRepository
				.save(ImageFixture.create(FilePurpose.REVIEW_IMAGE, "reviews/multi-2.png", fileUuid2, "multi-2.png"));

			var request = ReviewRequestFixture.createRequest(group.getId(), List.of(keyword.getId()),
				List.of(fileUuid1, fileUuid2));

			ReviewCreateResponse created = reviewService.createReview(
				member.getId(),
				restaurant.getId(),
				request);

			List<DomainImage> before = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.REVIEW, List.of(created.id()));
			assertThat(before).hasSize(2);

			reviewService.deleteReview(member.getId(), created.id());

			List<DomainImage> after = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.REVIEW, List.of(created.id()));
			assertThat(after).isEmpty();
		}

		@Test
		@DisplayName("이미지가 없는 리뷰를 삭제하면 오류 없이 완료된다")
		void deleteReview_withoutImage_completesWithoutError() {
			var request = ReviewRequestFixture.createRequest(group.getId(), List.of(keyword.getId()));

			ReviewCreateResponse created = reviewService.createReview(
				member.getId(),
				restaurant.getId(),
				request);

			reviewService.deleteReview(member.getId(), created.id());

			List<DomainImage> after = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.REVIEW, List.of(created.id()));
			assertThat(after).isEmpty();
		}

		@Test
		@DisplayName("다른 사용자가 리뷰를 삭제하려 하면 실패한다")
		void deleteReview_withDifferentMember_fails() {
			var request = ReviewRequestFixture.createRequest(group.getId(), List.of(keyword.getId()));

			ReviewCreateResponse created = reviewService.createReview(
				member.getId(),
				restaurant.getId(),
				request);

			Member otherMember = memberRepository.save(MemberFixture.create("other@example.com", "other"));

			assertThatThrownBy(() -> reviewService.deleteReview(otherMember.getId(), created.id()))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(CommonErrorCode.NO_PERMISSION.name());
		}
	}

	private Restaurant createRestaurant() {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			"테스트 음식점",
			"서울시 강남구 테헤란로 123",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-8888-9999");
	}
}
