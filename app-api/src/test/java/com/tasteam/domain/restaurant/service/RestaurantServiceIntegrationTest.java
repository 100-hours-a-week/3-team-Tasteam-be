package com.tasteam.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.dto.request.RestaurantCreateRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantUpdateRequest;
import com.tasteam.domain.restaurant.dto.response.RestaurantCreateResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantUpdateResponse;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;

@ServiceIntegrationTest
@Transactional
class RestaurantServiceIntegrationTest {

	@Autowired
	private RestaurantService restaurantService;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

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
		@DisplayName("이미지 없이 음식점을 생성한다")
		void createRestaurantWithoutImages() {
			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"맛있는 식당",
				"서울시 강남구 역삼동 123",
				"02-1111-2222",
				null,
				null,
				null);

			RestaurantCreateResponse response = restaurantService.createRestaurant(request);

			assertThat(response.id()).isNotNull();
			assertThat(response.createdAt()).isNotNull();

			Restaurant saved = restaurantRepository.findById(response.id()).orElseThrow();
			assertThat(saved.getName()).isEqualTo("맛있는 식당");
		}

		@Test
		@DisplayName("이미지와 함께 음식점을 생성하면 이미지가 ACTIVE 상태로 변경된다")
		void createRestaurantWithImages() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(
				Image.create(FilePurpose.RESTAURANT_IMAGE, "restaurant.png", 1024L, "image/png",
					"restaurants/restaurant.png", fileUuid));

			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"이미지 있는 식당",
				"서울시 강남구 역삼동 456",
				"02-2222-3333",
				null,
				List.of(fileUuid),
				null);

			RestaurantCreateResponse response = restaurantService.createRestaurant(request);

			assertThat(response.id()).isNotNull();

			Image updatedImage = imageRepository.findByFileUuid(fileUuid).orElseThrow();
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
		@DisplayName("음식점 상세를 조회하면 이미지가 포함된다")
		void getRestaurantDetailWithImage() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(
				Image.create(FilePurpose.RESTAURANT_IMAGE, "restaurant.png", 1024L, "image/png",
					"restaurants/restaurant.png", fileUuid));

			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"상세 조회 식당",
				"서울시 강남구 역삼동 789",
				"02-3333-4444",
				null,
				List.of(fileUuid),
				null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(request);

			RestaurantDetailResponse detail = restaurantService.getRestaurantDetail(created.id());

			assertThat(detail.id()).isEqualTo(created.id());
			assertThat(detail.name()).isEqualTo("상세 조회 식당");
			assertThat(detail.image()).isNotNull();
			assertThat(detail.image().url()).contains("restaurant.png");
		}
	}

	@Nested
	@DisplayName("음식점 수정")
	class UpdateRestaurant {

		@Test
		@DisplayName("음식점 이미지를 수정하면 기존 이미지가 삭제되고 새 이미지가 등록된다")
		void updateRestaurantImages() {
			UUID oldFileUuid = UUID.randomUUID();
			imageRepository.save(
				Image.create(FilePurpose.RESTAURANT_IMAGE, "old.png", 1024L, "image/png",
					"restaurants/old.png", oldFileUuid));

			RestaurantCreateRequest createRequest = new RestaurantCreateRequest(
				"수정할 식당",
				"서울시 강남구 역삼동 111",
				"02-4444-5555",
				null,
				List.of(oldFileUuid),
				null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(createRequest);

			UUID newFileUuid = UUID.randomUUID();
			imageRepository.save(
				Image.create(FilePurpose.RESTAURANT_IMAGE, "new.png", 2048L, "image/png",
					"restaurants/new.png", newFileUuid));

			RestaurantUpdateRequest updateRequest = new RestaurantUpdateRequest(
				"수정된 식당",
				null,
				List.of(newFileUuid));

			RestaurantUpdateResponse response = restaurantService.updateRestaurant(created.id(), updateRequest);

			assertThat(response.id()).isEqualTo(created.id());

			Image newImage = imageRepository.findByFileUuid(newFileUuid).orElseThrow();
			assertThat(newImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.RESTAURANT, List.of(created.id()));
			assertThat(domainImages).hasSize(1);
			assertThat(domainImages.get(0).getImage().getFileUuid()).isEqualTo(newFileUuid);
		}
	}

	@Nested
	@DisplayName("음식점 삭제")
	class DeleteRestaurant {

		@Test
		@DisplayName("음식점을 삭제하면 soft delete 처리된다")
		void deleteRestaurant() {
			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"삭제할 식당",
				"서울시 강남구 역삼동 222",
				"02-5555-6666",
				null,
				null,
				null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(request);

			restaurantService.deleteRestaurant(created.id());

			Restaurant deleted = restaurantRepository.findById(created.id()).orElseThrow();
			assertThat(deleted.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("음식점을 삭제하면 연관된 이미지도 삭제된다")
		void deleteRestaurantWithImages() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(
				Image.create(FilePurpose.RESTAURANT_IMAGE, "delete.png", 1024L, "image/png",
					"restaurants/delete.png", fileUuid));

			RestaurantCreateRequest request = new RestaurantCreateRequest(
				"이미지와 함께 삭제할 식당",
				"서울시 강남구 역삼동 333",
				"02-6666-7777",
				null,
				List.of(fileUuid),
				null);

			RestaurantCreateResponse created = restaurantService.createRestaurant(request);

			restaurantService.deleteRestaurant(created.id());

			List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.RESTAURANT, created.id());
			assertThat(domainImages).isEmpty();
		}
	}

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"서울시 강남구 테헤란로 123",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-7777-8888");
	}
}
