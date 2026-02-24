package com.tasteam.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.dto.request.RestaurantCreateRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantUpdateRequest;
import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;
import com.tasteam.domain.restaurant.dto.response.RestaurantCreateResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantUpdateResponse;
import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;
import com.tasteam.domain.restaurant.entity.FoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.entity.RestaurantComparison;
import com.tasteam.domain.restaurant.event.RestaurantEventPublisher;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.domain.restaurant.repository.AiRestaurantReviewAnalysisRepository;
import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantAddressRepository;
import com.tasteam.domain.restaurant.repository.RestaurantComparisonRepository;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.RestaurantWeeklyScheduleRepository;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.fixture.RestaurantRequestFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.FileErrorCode;

@ServiceIntegrationTest
@Transactional
class RestaurantServiceIntegrationTest {

	private static final UUID RESTAURANT_IMAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID RESTAURANT_IMAGE_UUID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID RESTAURANT_IMAGE_UUID_3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID RESTAURANT_IMAGE_UUID_4 = UUID.fromString("44444444-4444-4444-4444-444444444444");
	private static final UUID RESTAURANT_IMAGE_UUID_5 = UUID.fromString("55555555-5555-5555-5555-555555555555");
	private static final UUID RESTAURANT_IMAGE_UUID_MISSING = UUID.fromString("66666666-6666-6666-6666-666666666666");

	@Autowired
	private RestaurantService restaurantService;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private RestaurantAddressRepository restaurantAddressRepository;

	@Autowired
	private RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;

	@Autowired
	private RestaurantWeeklyScheduleRepository restaurantWeeklyScheduleRepository;

	@Autowired
	private FoodCategoryRepository foodCategoryRepository;

	@Autowired
	private AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;

	@Autowired
	private RestaurantComparisonRepository restaurantComparisonRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	@MockitoBean
	private RestaurantEventPublisher eventPublisher;

	@MockitoBean
	private NaverGeocodingClient naverGeocodingClient;

	private GeocodingResult mockGeocodingResult;

	@BeforeEach
	void setUp() {
		mockGeocodingResult = new GeocodingResult(
			"서울특별시",
			"강남구",
			"역삼동",
			"06234",
			127.0365,
			37.4979);
		given(naverGeocodingClient.geocode(anyString())).willReturn(mockGeocodingResult);
	}

	@Nested
	@DisplayName("음식점 생성")
	class CreateRestaurant {

		@Test
		@DisplayName("유효한 생성 요청이면 음식점과 연관 엔티티가 모두 생성되고 식별자를 반환한다")
		void createRestaurant_createsAllEntitiesAndReturnsIdentifier() {
			createAndSaveImage(RESTAURANT_IMAGE_UUID, "restaurants/main.png", "main.png");
			createAndSaveImage(RESTAURANT_IMAGE_UUID_2, "restaurants/sub.png", "sub.png");

			FoodCategory korean = foodCategoryRepository.save(FoodCategory.create("한식"));
			FoodCategory western = foodCategoryRepository.save(FoodCategory.create("양식"));

			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"종합 생성 식당",
				"서울시 강남구 역삼동 777",
				"02-7777-8888",
				List.of(korean.getId(), western.getId()),
				List.of(RESTAURANT_IMAGE_UUID, RESTAURANT_IMAGE_UUID_2),
				List.of(
					new WeeklyScheduleRequest(1, LocalTime.of(9, 0), LocalTime.of(21, 0), false,
						LocalDate.of(2026, 1, 1), null),
					new WeeklyScheduleRequest(2, null, null, true, LocalDate.of(2026, 1, 1), null)));

			RestaurantCreateResponse response = restaurantService.createRestaurant(request);

			assertThat(response.id()).isNotNull();
			assertThat(response.createdAt()).isNotNull();

			Restaurant savedRestaurant = restaurantRepository.findById(response.id()).orElseThrow();
			assertThat(savedRestaurant.getName()).isEqualTo("종합 생성 식당");
			assertThat(savedRestaurant.getPhoneNumber()).isEqualTo("02-7777-8888");

			assertThat(restaurantAddressRepository.existsByRestaurantId(response.id())).isTrue();
			assertThat(restaurantFoodCategoryRepository.countByRestaurantId(response.id())).isEqualTo(2);
			assertThat(restaurantWeeklyScheduleRepository.countByRestaurantId(response.id())).isEqualTo(2);
			assertThat(domainImageRepository.countByDomainTypeAndDomainId(DomainType.RESTAURANT, response.id()))
				.isEqualTo(2);

			Image firstImage = imageRepository.findByFileUuid(RESTAURANT_IMAGE_UUID).orElseThrow();
			Image secondImage = imageRepository.findByFileUuid(RESTAURANT_IMAGE_UUID_2).orElseThrow();
			assertThat(firstImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);
			assertThat(secondImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);
		}

		@Test
		@DisplayName("이미지 없이 음식점을 생성한다")
		void createRestaurantWithoutImages() {
			var request = RestaurantRequestFixture.createRestaurantRequest(
				"맛있는 식당", "서울시 강남구 역삼동 123", "02-1111-2222", null);

			RestaurantCreateResponse response = restaurantService.createRestaurant(request);

			assertThat(response.id()).isNotNull();
			assertThat(response.createdAt()).isNotNull();

			Restaurant saved = restaurantRepository.findById(response.id()).orElseThrow();
			assertThat(saved.getName()).isEqualTo("맛있는 식당");
		}

		@Test
		@DisplayName("선택 입력이 없으면 연관 엔티티를 만들지 않고 생성한다")
		void createRestaurant_withOptionalFieldsMissing_createsConditionally() {
			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"선택값 없는 식당",
				"서울시 강남구 역삼동 100",
				null,
				null,
				null,
				null);

			RestaurantCreateResponse response = restaurantService.createRestaurant(request);

			assertThat(response.id()).isNotNull();
			assertThat(response.createdAt()).isNotNull();

			Restaurant saved = restaurantRepository.findById(response.id()).orElseThrow();
			assertThat(saved.getPhoneNumber()).isNull();

			assertThat(restaurantAddressRepository.existsByRestaurantId(response.id())).isTrue();
			assertThat(restaurantFoodCategoryRepository.countByRestaurantId(response.id())).isZero();
			assertThat(restaurantWeeklyScheduleRepository.countByRestaurantId(response.id())).isZero();
			assertThat(domainImageRepository.countByDomainTypeAndDomainId(DomainType.RESTAURANT, response.id()))
				.isZero();
		}

		@Test
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		@DisplayName("중간 단계에서 실패하면 전체 생성이 롤백된다")
		void createRestaurant_rollsBackWhenIntermediateFailureOccurs() {
			FoodCategory korean = foodCategoryRepository.save(FoodCategory.create("롤백한식"));

			long restaurantCountBefore = restaurantRepository.count();
			long addressCountBefore = restaurantAddressRepository.count();
			long mappingCountBefore = restaurantFoodCategoryRepository.count();
			long scheduleCountBefore = restaurantWeeklyScheduleRepository.count();
			long domainImageCountBefore = domainImageRepository.count();

			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"롤백 식당",
				"서울시 강남구 역삼동 101",
				"02-1010-1010",
				List.of(korean.getId()),
				List.of(RESTAURANT_IMAGE_UUID_MISSING),
				List.of(new WeeklyScheduleRequest(3, LocalTime.of(10, 0), LocalTime.of(20, 0), false, null, null)));

			assertThatThrownBy(() -> restaurantService.createRestaurant(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(FileErrorCode.FILE_NOT_FOUND.name());

			assertThat(restaurantRepository.count()).isEqualTo(restaurantCountBefore);
			assertThat(restaurantAddressRepository.count()).isEqualTo(addressCountBefore);
			assertThat(restaurantFoodCategoryRepository.count()).isEqualTo(mappingCountBefore);
			assertThat(restaurantWeeklyScheduleRepository.count()).isEqualTo(scheduleCountBefore);
			assertThat(domainImageRepository.count()).isEqualTo(domainImageCountBefore);
		}

		@Test
		@DisplayName("음식점 생성이 완료되면 생성 이벤트를 발행한다")
		void createRestaurant_publishesCreatedEvent() {
			var request = RestaurantRequestFixture.createRestaurantRequest(
				"이벤트 식당", "서울시 강남구 역삼동 202", "02-2020-2020", null);

			RestaurantCreateResponse response = restaurantService.createRestaurant(request);

			verify(eventPublisher, times(1)).publishRestaurantCreated(response.id());
		}

		@Test
		@DisplayName("이미지와 함께 음식점을 생성하면 이미지가 ACTIVE 상태로 변경된다")
		void createRestaurantWithImages() {
			createAndSaveImage(RESTAURANT_IMAGE_UUID, "restaurants/restaurant.png", "restaurant.png");

			var request = RestaurantRequestFixture.createRestaurantRequest(
				"이미지 있는 식당", "서울시 강남구 역삼동 456", "02-2222-3333", List.of(RESTAURANT_IMAGE_UUID));

			RestaurantCreateResponse response = restaurantService.createRestaurant(request);

			assertThat(response.id()).isNotNull();

			Image updatedImage = imageRepository.findByFileUuid(RESTAURANT_IMAGE_UUID).orElseThrow();
			assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.RESTAURANT, List.of(response.id()));
			assertThat(domainImages).hasSize(1);
		}
	}

	@Nested
	@DisplayName("음식점 상세 조회")
	class GetRestaurantDetail {

		@Test
		@DisplayName("상세 조회 시 조합된 정보가 응답에 반영된다")
		void getRestaurantDetail_combinesComposedData() {
			createAndSaveImage(RESTAURANT_IMAGE_UUID_2, "restaurants/restaurant.png", "restaurant.png");

			FoodCategory korean = foodCategoryRepository.save(FoodCategory.create("상세한식"));
			FoodCategory western = foodCategoryRepository.save(FoodCategory.create("상세양식"));

			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"상세 조회 식당",
				"서울시 강남구 역삼동 789",
				"02-3333-4444",
				List.of(korean.getId(), western.getId()),
				List.of(RESTAURANT_IMAGE_UUID_2),
				List.of(new WeeklyScheduleRequest(1, LocalTime.of(9, 0), LocalTime.of(22, 0), false, null, null)));

			RestaurantCreateResponse created = restaurantService.createRestaurant(request);
			Restaurant restaurant = restaurantRepository.findById(created.id()).orElseThrow();

			Member member = memberRepository.save(MemberFixture.create("detail-reviewer@example.com", "상세리뷰어"));
			reviewRepository.save(Review.create(restaurant, member, 1L, null, "추천", true));
			reviewRepository.save(Review.create(restaurant, member, 1L, null, "추천2", true));
			reviewRepository.save(Review.create(restaurant, member, 1L, null, "비추천", false));

			aiRestaurantReviewAnalysisRepository.save(
				AiRestaurantReviewAnalysis.create(created.id(), "AI 요약", BigDecimal.valueOf(0.75)));
			restaurantComparisonRepository.save(
				RestaurantComparison.create(
					restaurant.getId(),
					null,
					Map.of("comparison_display", List.of("AI 특징"), "category_lift", Map.of(), "total_candidates", 0,
						"validated_count", 0),
					Instant.now()));

			RestaurantDetailResponse detail = restaurantService.getRestaurantDetail(created.id());

			assertThat(detail.id()).isEqualTo(created.id());
			assertThat(detail.name()).isNotBlank();
			assertThat(detail.foodCategories()).isNotEmpty();
			assertThat(detail.businessHoursWeek()).hasSize(7);
			assertThat(detail.image()).isNotNull();
			assertThat(detail.recommendStat()).isNotNull();
			assertThat(detail.recommendStat().recommendedCount()).isEqualTo(2L);
			assertThat(detail.recommendStat().notRecommendedCount()).isEqualTo(1L);
			assertThat(detail.recommendStat().positiveRatio()).isNotNull();
			assertThat(detail.aiSummary()).isNotNull();
			assertThat(detail.aiFeatures()).isNotNull();
		}

		@Test
		@DisplayName("선택 데이터가 없으면 null 분기로 반환된다")
		void getRestaurantDetail_returnsNullForOptionalDataWhenAbsent() {
			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"옵셔널 없는 식당",
				"서울시 강남구 역삼동 790",
				"02-0000-0000",
				null,
				null,
				null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(request);

			RestaurantDetailResponse detail = restaurantService.getRestaurantDetail(created.id());

			assertThat(detail.id()).isEqualTo(created.id());
			assertThat(detail.businessHoursWeek()).hasSize(7);
			assertThat(detail.image()).isNull();
			assertThat(detail.aiSummary()).isNull();
			assertThat(detail.aiFeatures()).isNull();
			assertThat(detail.recommendStat()).isNotNull();
			assertThat(detail.recommendStat().positiveRatio()).isNull();
		}

		@Test
		@DisplayName("존재하지 않는 음식점 상세 조회는 예외가 발생한다")
		void getRestaurantDetail_notFound_throwsBusinessException() {
			assertThatThrownBy(() -> restaurantService.getRestaurantDetail(Long.MAX_VALUE))
				.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("음식점 수정")
	class UpdateRestaurant {

		@Test
		@DisplayName("이름만 수정하면 이름만 변경되고 나머지 상태는 유지된다")
		void updateRestaurant_nameOnly_changesOnlyTargetField() {
			var createRequest = RestaurantRequestFixture.createRestaurantRequest(
				"원래 식당", "서울시 강남구 역삼동 111", "02-4444-5555", null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(createRequest);
			Restaurant before = restaurantRepository.findById(created.id()).orElseThrow();
			String beforeAddress = before.getFullAddress();
			long beforeCategoryCount = restaurantFoodCategoryRepository.countByRestaurantId(created.id());
			long beforeImageCount = domainImageRepository.countByDomainTypeAndDomainId(DomainType.RESTAURANT,
				created.id());

			RestaurantUpdateRequest updateRequest = new RestaurantUpdateRequest(
				"이름만 변경",
				null,
				null);

			restaurantService.updateRestaurant(created.id(), updateRequest);

			Restaurant after = restaurantRepository.findById(created.id()).orElseThrow();
			assertThat(after.getName()).isEqualTo("이름만 변경");
			assertThat(after.getFullAddress()).isEqualTo(beforeAddress);
			assertThat(restaurantFoodCategoryRepository.countByRestaurantId(created.id()))
				.isEqualTo(beforeCategoryCount);
			assertThat(domainImageRepository.countByDomainTypeAndDomainId(DomainType.RESTAURANT, created.id()))
				.isEqualTo(beforeImageCount);
		}

		@Test
		@DisplayName("음식점 이미지를 수정하면 기존 이미지가 삭제되고 새 이미지가 등록된다")
		void updateRestaurantImages() {
			createAndSaveImage(RESTAURANT_IMAGE_UUID_3, "restaurants/old.png", "old.png");

			var createRequest = RestaurantRequestFixture.createRestaurantRequest(
				"수정할 식당", "서울시 강남구 역삼동 111", "02-4444-5555", List.of(RESTAURANT_IMAGE_UUID_3));

			RestaurantCreateResponse created = restaurantService.createRestaurant(createRequest);

			imageRepository.save(ImageFixture.create(FilePurpose.RESTAURANT_IMAGE, "restaurants/new.png",
				RESTAURANT_IMAGE_UUID_4, "new.png", 2048L));

			RestaurantUpdateRequest updateRequest = new RestaurantUpdateRequest(
				"수정된 식당",
				null,
				List.of(RESTAURANT_IMAGE_UUID_4));

			RestaurantUpdateResponse response = restaurantService.updateRestaurant(created.id(), updateRequest);

			assertThat(response.id()).isEqualTo(created.id());

			Image newImage = imageRepository.findByFileUuid(RESTAURANT_IMAGE_UUID_4).orElseThrow();
			assertThat(newImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.RESTAURANT, List.of(created.id()));
			assertThat(domainImages).hasSize(1);
			assertThat(domainImages.get(0).getImage().getFileUuid()).isEqualTo(RESTAURANT_IMAGE_UUID_4);
		}

		@Test
		@DisplayName("존재하지 않는 이미지를 지정하면 수정이 롤백된다")
		void updateRestaurant_withMissingImage_fails() {
			var createRequest = RestaurantRequestFixture.createRestaurantRequest(
				"이미지 누락 테스트", "서울시 강남구 역삼동 999", "02-9999-9999", null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(createRequest);
			Restaurant before = restaurantRepository.findById(created.id()).orElseThrow();
			long beforeImageCount = domainImageRepository.countByDomainTypeAndDomainId(DomainType.RESTAURANT,
				created.id());
			long beforeCategoryCount = restaurantFoodCategoryRepository.countByRestaurantId(created.id());

			RestaurantUpdateRequest updateRequest = new RestaurantUpdateRequest(
				"수정 시도 이름",
				null,
				List.of(RESTAURANT_IMAGE_UUID_MISSING));

			assertThatThrownBy(() -> restaurantService.updateRestaurant(created.id(), updateRequest))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(FileErrorCode.FILE_NOT_FOUND.name());

			Restaurant after = restaurantRepository.findById(created.id()).orElseThrow();
			assertThat(after.getName()).isEqualTo(before.getName());
			assertThat(domainImageRepository.countByDomainTypeAndDomainId(DomainType.RESTAURANT, created.id()))
				.isEqualTo(beforeImageCount);
			assertThat(restaurantFoodCategoryRepository.countByRestaurantId(created.id()))
				.isEqualTo(beforeCategoryCount);
		}

		@Test
		@DisplayName("존재하지 않는 음식점을 수정하면 예외가 발생한다")
		void updateRestaurant_notFound_throwsBusinessException() {
			RestaurantUpdateRequest updateRequest = new RestaurantUpdateRequest("없는 식당", null, null);

			assertThatThrownBy(() -> restaurantService.updateRestaurant(Long.MAX_VALUE, updateRequest))
				.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("음식점 삭제")
	class DeleteRestaurant {

		@Test
		@DisplayName("음식점을 삭제하면 soft delete 처리된다")
		void deleteRestaurant() {
			var request = RestaurantRequestFixture.createRestaurantRequest(
				"삭제할 식당", "서울시 강남구 역삼동 222", "02-5555-6666", null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(request);

			restaurantService.deleteRestaurant(created.id());

			Restaurant deleted = restaurantRepository.findById(created.id()).orElseThrow();
			assertThat(deleted.getDeletedAt()).isNotNull();
			assertThat(restaurantRepository.findByIdAndDeletedAtIsNull(created.id())).isEmpty();
		}

		@Test
		@DisplayName("음식점을 삭제하면 연관된 이미지도 삭제된다")
		void deleteRestaurantWithImages() {
			createAndSaveImage(RESTAURANT_IMAGE_UUID_5, "restaurants/delete.png", "delete.png");

			var request = RestaurantRequestFixture.createRestaurantRequest(
				"이미지와 함께 삭제할 식당", "서울시 강남구 역삼동 333", "02-6666-7777", List.of(RESTAURANT_IMAGE_UUID_5));

			RestaurantCreateResponse created = restaurantService.createRestaurant(request);

			restaurantService.deleteRestaurant(created.id());

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.RESTAURANT, created.id());
			assertThat(domainImages).isEmpty();
		}

		@Test
		@DisplayName("이미 삭제된 음식점을 다시 삭제해도 상태는 유지된다")
		void deleteRestaurant_idempotentWhenAlreadyDeleted() {
			var request = RestaurantRequestFixture.createRestaurantRequest(
				"재삭제 식당", "서울시 강남구 역삼동 334", "02-6666-8888", null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(request);
			restaurantService.deleteRestaurant(created.id());

			Instant firstDeletedAt = restaurantRepository.findById(created.id()).orElseThrow().getDeletedAt();
			restaurantService.deleteRestaurant(created.id());
			Instant secondDeletedAt = restaurantRepository.findById(created.id()).orElseThrow().getDeletedAt();

			assertThat(secondDeletedAt).isEqualTo(firstDeletedAt);
		}

		@Test
		@DisplayName("존재하지 않는 음식점을 삭제하면 아무 일도 일어나지 않는다")
		void deleteRestaurant_notFound_noop() {
			long beforeRestaurantCount = restaurantRepository.count();
			long beforeDomainImageCount = domainImageRepository.count();

			restaurantService.deleteRestaurant(Long.MAX_VALUE);

			assertThat(restaurantRepository.count()).isEqualTo(beforeRestaurantCount);
			assertThat(domainImageRepository.count()).isEqualTo(beforeDomainImageCount);
		}
	}

	private void createAndSaveImage(UUID fileUuid, String storageKey, String fileName) {
		imageRepository.save(ImageFixture.create(FilePurpose.RESTAURANT_IMAGE, storageKey, fileUuid, fileName));
	}
}
