package com.tasteam.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.admin.dto.request.AdminRestaurantCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminRestaurantUpdateRequest;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;
import com.tasteam.domain.restaurant.entity.FoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.entity.RestaurantFoodCategory;
import com.tasteam.domain.restaurant.entity.RestaurantWeeklySchedule;
import com.tasteam.domain.restaurant.event.RestaurantEventPublisher;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantAddressRepository;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.RestaurantWeeklyScheduleRepository;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.FileErrorCode;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.infra.storage.StorageClient;

@ServiceIntegrationTest
@Transactional
class AdminRestaurantServiceIntegrationTest {

	private static final UUID RESTAURANT_IMAGE_UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
	private static final UUID RESTAURANT_IMAGE_UUID_2 = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
	private static final UUID RESTAURANT_IMAGE_UUID_MISSING = UUID.fromString("cccccccc-3333-3333-3333-333333333333");

	@Autowired
	private AdminRestaurantService adminRestaurantService;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private RestaurantAddressRepository restaurantAddressRepository;

	@Autowired
	private RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;

	@Autowired
	private FoodCategoryRepository foodCategoryRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private RestaurantWeeklyScheduleRepository weeklyScheduleRepository;

	@MockitoBean
	private NaverGeocodingClient naverGeocodingClient;

	@MockitoBean
	private RestaurantEventPublisher restaurantEventPublisher;

	@MockitoBean
	private StorageClient storageClient;

	@BeforeEach
	void setUp() {
		given(naverGeocodingClient.geocode(anyString())).willReturn(
			new GeocodingResult("서울특별시", "강남구", "역삼동", "06234", 127.0365, 37.4979));
		given(storageClient.createPresignedGetUrl(anyString())).willReturn("https://cdn.test/restaurant.png");
	}

	@Nested
	@DisplayName("관리자 음식점 생성")
	class CreateRestaurant {

		@Test
		@DisplayName("주소 지오코딩 + 카테고리/이미지/주간 스케줄이 함께 저장된다")
		void createRestaurantWithCategoriesImagesAndSchedules() {
			FoodCategory category = foodCategoryRepository.save(FoodCategory.create("한식"));
			createAndSaveImage(RESTAURANT_IMAGE_UUID, "uploads/restaurant/image/admin-create.png", "admin-create.png");

			var request = new AdminRestaurantCreateRequest(
				"관리자 음식점",
				"서울특별시 강남구",
				"02-1111-2222",
				List.of(category.getId()),
				List.of(RESTAURANT_IMAGE_UUID),
				List.of(new WeeklyScheduleRequest(1, LocalTime.of(9, 0), LocalTime.of(22, 0), false, null, null)));

			Long restaurantId = adminRestaurantService.createRestaurant(request);

			Restaurant saved = restaurantRepository.findById(restaurantId).orElseThrow();
			assertThat(saved.getName()).isEqualTo("관리자 음식점");
			assertThat(restaurantAddressRepository.findByRestaurantId(restaurantId)).isPresent();

			List<RestaurantFoodCategory> mappings = restaurantFoodCategoryRepository.findByRestaurantId(restaurantId);
			assertThat(mappings).hasSize(1);

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.RESTAURANT, List.of(restaurantId));
			assertThat(domainImages).hasSize(1);

			Image image = imageRepository.findByFileUuid(RESTAURANT_IMAGE_UUID).orElseThrow();
			assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<RestaurantWeeklySchedule> schedules = weeklyScheduleRepository.findByRestaurantId(restaurantId);
			assertThat(schedules).hasSize(1);
		}

		@Test
		@DisplayName("존재하지 않는 이미지를 지정하면 음식점 생성에 실패한다")
		void createRestaurantWithMissingImageFails() {
			var request = new AdminRestaurantCreateRequest(
				"이미지 누락",
				"서울특별시 강남구",
				"02-3333-4444",
				List.of(),
				List.of(RESTAURANT_IMAGE_UUID_MISSING),
				List.of());

			assertThatThrownBy(() -> adminRestaurantService.createRestaurant(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(FileErrorCode.FILE_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("관리자 음식점 상세 조회")
	class GetRestaurantDetail {

		@Test
		@DisplayName("카테고리 및 이미지 URL이 포함된다")
		void getRestaurantDetailIncludesCategoriesAndImages() {
			FoodCategory category = foodCategoryRepository.save(FoodCategory.create("중식"));
			createAndSaveImage(RESTAURANT_IMAGE_UUID, "uploads/restaurant/image/admin-detail.png", "admin-detail.png");

			Long restaurantId = adminRestaurantService.createRestaurant(new AdminRestaurantCreateRequest(
				"상세 조회",
				"서울특별시 강남구",
				"02-5555-6666",
				List.of(category.getId()),
				List.of(RESTAURANT_IMAGE_UUID),
				List.of()));

			var response = adminRestaurantService.getRestaurantDetail(restaurantId);

			assertThat(response.name()).isEqualTo("상세 조회");
			assertThat(response.foodCategories()).hasSize(1);
			assertThat(response.images()).hasSize(1);
			assertThat(response.images().getFirst().url()).contains("admin-detail.png");
		}

		@Test
		@DisplayName("존재하지 않는 음식점이면 실패한다")
		void getRestaurantDetailNotFoundFails() {
			assertThatThrownBy(() -> adminRestaurantService.getRestaurantDetail(999999L))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(RestaurantErrorCode.RESTAURANT_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("관리자 음식점 수정")
	class UpdateRestaurant {

		@Test
		@DisplayName("카테고리 변경 및 이미지 재연결이 반영된다")
		void updateRestaurantUpdatesCategoriesAndImages() {
			Long restaurantId = adminRestaurantService.createRestaurant(new AdminRestaurantCreateRequest(
				"수정 대상",
				"서울특별시 강남구",
				"02-7777-8888",
				List.of(),
				List.of(),
				List.of()));

			FoodCategory category = foodCategoryRepository.save(FoodCategory.create("일식"));
			createAndSaveImage(RESTAURANT_IMAGE_UUID_2, "uploads/restaurant/image/admin-update.png",
				"admin-update.png");

			adminRestaurantService.updateRestaurant(
				restaurantId,
				new AdminRestaurantUpdateRequest("수정됨", null, List.of(category.getId()),
					List.of(RESTAURANT_IMAGE_UUID_2)));

			List<RestaurantFoodCategory> mappings = restaurantFoodCategoryRepository.findByRestaurantId(restaurantId);
			assertThat(mappings).hasSize(1);

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.RESTAURANT, List.of(restaurantId));
			assertThat(domainImages).hasSize(1);
		}

		@Test
		@DisplayName("존재하지 않는 음식점이면 수정에 실패한다")
		void updateRestaurantNotFoundFails() {
			assertThatThrownBy(() -> adminRestaurantService.updateRestaurant(
				999999L,
				new AdminRestaurantUpdateRequest("수정", null, List.of(), List.of())))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(RestaurantErrorCode.RESTAURANT_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("관리자 음식점 삭제")
	class DeleteRestaurant {

		@Test
		@DisplayName("soft delete 처리 및 이미지 연결이 제거된다")
		void deleteRestaurantSoftDeletesAndRemovesImages() {
			createAndSaveImage(RESTAURANT_IMAGE_UUID, "uploads/restaurant/image/admin-delete.png", "admin-delete.png");

			Long restaurantId = adminRestaurantService.createRestaurant(new AdminRestaurantCreateRequest(
				"삭제 대상",
				"서울특별시 강남구",
				"02-9999-0000",
				List.of(),
				List.of(RESTAURANT_IMAGE_UUID),
				List.of()));

			adminRestaurantService.deleteRestaurant(restaurantId);

			Restaurant deleted = restaurantRepository.findById(restaurantId).orElseThrow();
			assertThat(deleted.getDeletedAt()).isNotNull();

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.RESTAURANT, restaurantId);
			assertThat(domainImages).isEmpty();
		}
	}

	private void createAndSaveImage(UUID fileUuid, String storageKey, String fileName) {
		imageRepository.save(ImageFixture.create(FilePurpose.RESTAURANT_IMAGE, storageKey, fileUuid, fileName));
	}
}
