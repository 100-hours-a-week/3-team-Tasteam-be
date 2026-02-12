package com.tasteam.domain.admin.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.request.AdminRestaurantCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminRestaurantSearchCondition;
import com.tasteam.domain.admin.dto.request.AdminRestaurantUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminRestaurantDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminRestaurantListItem;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.service.DomainImageLinker;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.entity.FoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.entity.RestaurantAddress;
import com.tasteam.domain.restaurant.entity.RestaurantFoodCategory;
import com.tasteam.domain.restaurant.entity.RestaurantWeeklySchedule;
import com.tasteam.domain.restaurant.event.RestaurantEventPublisher;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantAddressRepository;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantQueryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.RestaurantWeeklyScheduleRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;
import com.tasteam.domain.restaurant.validator.RestaurantFoodCategoryValidator;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.infra.storage.StorageClient;
import com.tasteam.infra.storage.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRestaurantService {

	private final RestaurantRepository restaurantRepository;
	private final RestaurantQueryRepository restaurantQueryRepository;
	private final RestaurantAddressRepository restaurantAddressRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final FoodCategoryRepository foodCategoryRepository;
	private final RestaurantFoodCategoryValidator restaurantFoodCategoryValidator;
	private final DomainImageRepository domainImageRepository;
	private final GeometryFactory geometryFactory;
	private final NaverGeocodingClient naverGeocodingClient;
	private final RestaurantWeeklyScheduleRepository weeklyScheduleRepository;
	private final RestaurantEventPublisher eventPublisher;
	private final StorageProperties storageProperties;
	private final StorageClient storageClient;
	private final DomainImageLinker domainImageLinker;

	@Transactional(readOnly = true)
	public Page<AdminRestaurantListItem> getRestaurants(
		AdminRestaurantSearchCondition condition,
		Pageable pageable) {

		Page<Restaurant> restaurants = restaurantQueryRepository
			.findAllByAdminCondition(condition, pageable);

		List<Long> restaurantIds = restaurants.stream()
			.map(Restaurant::getId)
			.toList();

		Map<Long, List<String>> categoryMap = restaurantFoodCategoryRepository
			.findCategoriesByRestaurantIds(restaurantIds)
			.stream()
			.collect(Collectors.groupingBy(
				RestaurantCategoryProjection::getRestaurantId,
				Collectors.mapping(
					RestaurantCategoryProjection::getCategoryName,
					Collectors.toList())));

		return restaurants.map(r -> new AdminRestaurantListItem(
			r.getId(),
			r.getName(),
			r.getFullAddress(),
			categoryMap.getOrDefault(r.getId(), List.of()),
			r.getCreatedAt(),
			r.getDeletedAt()));
	}

	@Transactional(readOnly = true)
	public AdminRestaurantDetailResponse getRestaurantDetail(Long restaurantId) {
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		List<RestaurantFoodCategory> foodCategories = restaurantFoodCategoryRepository
			.findByRestaurantId(restaurantId);

		List<AdminRestaurantDetailResponse.FoodCategoryInfo> categoryInfos = foodCategories.stream()
			.map(rfc -> new AdminRestaurantDetailResponse.FoodCategoryInfo(
				rfc.getFoodCategory().getId(),
				rfc.getFoodCategory().getName(),
				null))
			.toList();

		List<DomainImage> domainImages = domainImageRepository
			.findAllByDomainTypeAndDomainIdIn(DomainType.RESTAURANT, List.of(restaurantId));

		List<RestaurantImageDto> images = domainImages.stream()
			.map(di -> new RestaurantImageDto(
				di.getImage().getId(),
				buildPublicUrl(di.getImage().getStorageKey())))
			.toList();

		Point location = restaurant.getLocation();
		Double latitude = location != null ? location.getY() : null;
		Double longitude = location != null ? location.getX() : null;

		return new AdminRestaurantDetailResponse(
			restaurant.getId(),
			restaurant.getName(),
			restaurant.getFullAddress(),
			latitude,
			longitude,
			categoryInfos,
			images,
			restaurant.getCreatedAt(),
			restaurant.getUpdatedAt(),
			restaurant.getDeletedAt());
	}

	@Transactional
	public Long createRestaurant(AdminRestaurantCreateRequest request) {

		List<Long> categoryIds = request.foodCategoryIds() == null
			? List.of()
			: request.foodCategoryIds();

		restaurantFoodCategoryValidator.validate(categoryIds);

		Set<Long> foodCategoryIdSet = categoryIds.stream()
			.collect(Collectors.toUnmodifiableSet());

		GeocodingResult result = naverGeocodingClient.geocode(request.address());

		Coordinate coordinate = new Coordinate(result.longitude(), result.latitude());
		Point location = geometryFactory.createPoint(coordinate);

		Restaurant restaurant = Restaurant.create(request.name(), request.address(), location, request.phoneNumber());
		restaurantRepository.save(restaurant);

		RestaurantAddress restaurantAddress = RestaurantAddress.create(
			restaurant,
			result.sido(),
			result.sigungu(),
			result.eupmyeondong(),
			result.postalCode());
		restaurantAddressRepository.save(restaurantAddress);

		if (!foodCategoryIdSet.isEmpty()) {
			List<FoodCategory> categories = foodCategoryRepository.findAllById(foodCategoryIdSet);
			List<RestaurantFoodCategory> mappings = categories.stream()
				.map(category -> RestaurantFoodCategory.create(restaurant, category))
				.toList();
			restaurantFoodCategoryRepository.saveAll(mappings);
		}

		saveImagesIfPresent(restaurant, request.imageIds());

		saveWeeklySchedulesIfPresent(restaurant, request.weeklySchedules());

		eventPublisher.publishRestaurantCreated(restaurant.getId());

		return restaurant.getId();
	}

	@Transactional
	public void updateRestaurant(Long restaurantId, AdminRestaurantUpdateRequest request) {

		Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		if (request.name() != null && !request.name().equals(restaurant.getName())) {
			restaurant.changeName(request.name());
		}

		if (request.foodCategoryIds() != null) {
			restaurantFoodCategoryValidator.validate(request.foodCategoryIds());

			Set<Long> foodCategoryIdSet = request.foodCategoryIds().stream()
				.collect(Collectors.toUnmodifiableSet());

			restaurantFoodCategoryRepository.deleteByRestaurantId(restaurantId);

			List<FoodCategory> categories = foodCategoryRepository.findAllById(foodCategoryIdSet);
			List<RestaurantFoodCategory> mappings = categories.stream()
				.map(category -> RestaurantFoodCategory.create(restaurant, category))
				.toList();
			restaurantFoodCategoryRepository.saveAll(mappings);
		}

		if (request.imageIds() != null) {
			domainImageRepository.deleteAllByDomainTypeAndDomainId(DomainType.RESTAURANT, restaurantId);
			saveImagesIfPresent(restaurant, request.imageIds());
		}
	}

	@Transactional
	public void deleteRestaurant(Long restaurantId) {
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		if (restaurant.getDeletedAt() != null) {
			return;
		}

		Instant now = Instant.now();
		restaurant.softDelete(now);
		domainImageRepository.deleteAllByDomainTypeAndDomainId(DomainType.RESTAURANT, restaurantId);
	}

	private void saveImagesIfPresent(Restaurant restaurant, List<UUID> imageIds) {
		domainImageLinker.linkImages(DomainType.RESTAURANT, restaurant.getId(), imageIds);
	}

	private void saveWeeklySchedulesIfPresent(Restaurant restaurant, List<WeeklyScheduleRequest> schedules) {
		if (schedules == null || schedules.isEmpty()) {
			return;
		}

		List<RestaurantWeeklySchedule> weeklySchedules = schedules.stream()
			.map(s -> RestaurantWeeklySchedule.create(
				restaurant,
				s.dayOfWeek(),
				s.openTime(),
				s.closeTime(),
				s.isClosed(),
				s.effectiveFrom(),
				s.effectiveTo()))
			.toList();
		weeklyScheduleRepository.saveAll(weeklySchedules);
	}

	private String buildPublicUrl(String storageKey) {
		if (storageProperties.isPresignedAccess()) {
			return storageClient.createPresignedGetUrl(storageKey);
		}
		String baseUrl = storageProperties.getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = String.format("https://%s.s3.%s.amazonaws.com",
				storageProperties.getBucket(),
				storageProperties.getRegion());
		}
		String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		String normalizedKey = storageKey.startsWith("/") ? storageKey.substring(1) : storageKey;
		return normalizedBase + "/" + normalizedKey;
	}
}
