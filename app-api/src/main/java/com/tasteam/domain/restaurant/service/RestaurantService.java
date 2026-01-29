package com.tasteam.domain.restaurant.service;

import static java.util.stream.Collectors.groupingBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.dto.GroupRestaurantSearchCondition;
import com.tasteam.domain.restaurant.dto.RestaurantCursor;
import com.tasteam.domain.restaurant.dto.RestaurantDistanceQueryDto;
import com.tasteam.domain.restaurant.dto.request.NearbyRestaurantQueryParams;
import com.tasteam.domain.restaurant.dto.request.RestaurantCreateRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantUpdateRequest;
import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;
import com.tasteam.domain.restaurant.dto.response.BusinessHourWeekItem;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantCreateResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse.RecommendStatResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.dto.response.RestaurantListItem;
import com.tasteam.domain.restaurant.dto.response.RestaurantUpdateResponse;
import com.tasteam.domain.restaurant.entity.*;
import com.tasteam.domain.restaurant.event.RestaurantEventPublisher;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.domain.restaurant.policy.RestaurantSearchPolicy;
import com.tasteam.domain.restaurant.repository.*;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantImageProjection;
import com.tasteam.domain.restaurant.validator.GroupRestaurantSearchConditionValidator;
import com.tasteam.domain.restaurant.validator.RestaurantFoodCategoryValidator;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.global.utils.CursorCodec;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RestaurantService {

	private final RestaurantRepository restaurantRepository;
	private final RestaurantQueryRepository restaurantQueryRepository;
	private final RestaurantAddressRepository restaurantAddressRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final RestaurantImageRepository restaurantImageRepository;
	private final FoodCategoryRepository foodCategoryRepository;
	private final AiRestaurantFeatureRepository aiRestaurantFeatureRepository;
	private final AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;
	private final ReviewRepository reviewRepository;
	private final ImageRepository imageRepository;
	private final RestaurantEventPublisher eventPublisher;
	private final CursorCodec cursorCodec;
	private final GroupRestaurantSearchConditionValidator groupRestaurantSearchConditionValidator;
	private final RestaurantFoodCategoryValidator restaurantFoodCategoryValidator;
	private final GeometryFactory geometryFactory;
	private final NaverGeocodingClient naverGeocodingClient;
	private final RestaurantScheduleService restaurantScheduleService;
	private final RestaurantWeeklyScheduleRepository weeklyScheduleRepository;

	@Transactional(readOnly = true)
	public RestaurantDetailResponse getRestaurantDetail(long restaurantId) {

		// 음식점
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		// 음식 카테고리
		List<String> foodCategories = restaurantFoodCategoryRepository.findByRestaurantId(restaurantId)
			.stream()
			.map(RestaurantFoodCategory::getFoodCategory)
			.map(FoodCategory::getName)
			.toList();

		List<BusinessHourWeekItem> businessHoursWeek = restaurantScheduleService.getBusinessHoursWeek(restaurantId);

		// 음식점 대표 이미지 (최대 1장)
		RestaurantImage firstImage = restaurantImageRepository
			.findByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId)
			.getFirst();
		RestaurantImageDto image = new RestaurantImageDto(firstImage.getId(), firstImage.getImageUrl());

		// AI 요약
		Optional<AiRestaurantReviewAnalysis> aiAnalysis = aiRestaurantReviewAnalysisRepository
			.findByRestaurantId(restaurantId);
		String aiSummary = aiAnalysis.map(AiRestaurantReviewAnalysis::getSummary).orElse(null);

		// AI Feature
		String aiFeature = aiRestaurantFeatureRepository.findByRestaurantId(restaurantId)
			.map(AiRestaurantFeature::getContent)
			.orElse(null);

		// 추천 통계
		long recommendCount = reviewRepository.countByRestaurantIdAndIsRecommendedTrueAndDeletedAtIsNull(restaurantId);
		long notRecommendedCount = reviewRepository
			.countByRestaurantIdAndIsRecommendedFalseAndDeletedAtIsNull(restaurantId);
		Long positiveRatio = aiAnalysis
			.map(AiRestaurantReviewAnalysis::getPositiveReviewRatio)
			.map(ratio -> ratio.multiply(BigDecimal.valueOf(100))
				.setScale(0, RoundingMode.HALF_UP)
				.longValueExact())
			.orElse(null);

		RecommendStatResponse recommendStatResponse = new RecommendStatResponse(
			recommendCount,
			notRecommendedCount,
			positiveRatio);

		return new RestaurantDetailResponse(
			restaurant.getId(),
			restaurant.getName(),
			restaurant.getFullAddress(),
			foodCategories,
			businessHoursWeek,
			image,
			null,
			recommendStatResponse,
			aiSummary,
			aiFeature,
			restaurant.getCreatedAt(),
			restaurant.getUpdatedAt());
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<RestaurantListItem> getGroupRestaurants(
		long groupId,
		NearbyRestaurantQueryParams queryParam) {

		/*
		if (groupRepository
				.existsByIdAndDeletedAtIsNull(groupId)) {
			throw new BusinessException(GroupErrorCode.GROUP_NOT_FOUND);
		}
		 */

		// 커서 변환
		RestaurantCursor cursor = cursorCodec.decodeOrNull(queryParam.cursor(), RestaurantCursor.class);
		if (queryParam.cursor() != null && cursor == null) {
			// 유효하지 않은 커서인 경우 다음 페이지 없음으로 해석
			return CursorPageResponse.empty();
		}

		// 검색 조건 생성 및 검증
		GroupRestaurantSearchCondition condition = toGroupRestaurantSearchCondition(groupId, queryParam, cursor);
		groupRestaurantSearchConditionValidator.validate(condition);

		// 음식점 검색 결과
		CursorQueryResult<RestaurantDistanceQueryDto> result = searchNearbyGroupRestaurant(condition);

		// 음식점 아이디 목록
		List<Long> restaurantIds = result.items().stream()
			.map(RestaurantDistanceQueryDto::id)
			.toList();

		// 음식점 대표 이미지 (최대 3장)
		Map<Long, List<RestaurantImageDto>> restaurantThumbnails = restaurantImageRepository
			.findRestaurantImages(restaurantIds)
			.stream()
			.collect(Collectors.groupingBy(
				RestaurantImageProjection::getRestaurantId,
				Collectors.collectingAndThen(
					Collectors.mapping(
						p -> new RestaurantImageDto(
							p.getImageId(),
							p.getImageUrl()),
						Collectors.toList()),
					list -> list.size() > 3 ? list.subList(0, 3) : list)));

		// 음식점 음식 카테고리
		Map<Long, List<String>> restaurantCategories = restaurantFoodCategoryRepository
			.findCategoriesByRestaurantIds(restaurantIds)
			.stream()
			.collect(groupingBy(
				RestaurantCategoryProjection::getRestaurantId,
				Collectors.mapping(
					RestaurantCategoryProjection::getCategoryName,
					Collectors.toList())));

		// 음식점 정보 조립
		List<RestaurantListItem> items = result.items().stream()
			.map(r -> new RestaurantListItem(
				r.id(),
				r.name(),
				r.address(),
				r.distanceMeter(),
				restaurantCategories.getOrDefault(r.id(), List.of()),
				restaurantThumbnails.get(r.id())))
			.toList();

		// 그룹 음식점 목록 조회 결과 조립
		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				result.nextCursor(),
				result.hasNext(),
				result.size()));
	}

	public CursorQueryResult<RestaurantDistanceQueryDto> searchNearbyGroupRestaurant(
		GroupRestaurantSearchCondition condition) {
		// 거리순 음식점 페이징 조회
		List<RestaurantDistanceQueryDto> pageResult = restaurantQueryRepository
			.findRestaurantsWithDistance(
				condition.groupId(),
				condition.latitude(),
				condition.longitude(),
				condition.radiusMeter(),
				condition.foodCategories(),
				condition.cursor(),
				condition.pageSize() + 1);

		boolean hasNext = pageResult.size() > condition.pageSize();
		List<RestaurantDistanceQueryDto> pageContent = hasNext ? pageResult.subList(0, condition.pageSize())
			: pageResult;

		String nextCursor = null;
		if (hasNext) {
			RestaurantDistanceQueryDto last = pageContent.getLast();
			nextCursor = cursorCodec.encode(
				new RestaurantCursor(last.distanceMeter(), last.id()));
		}

		return new CursorQueryResult<>(
			pageContent,
			nextCursor,
			hasNext,
			pageContent.size());
	}

	@Transactional
	public RestaurantCreateResponse createRestaurant(RestaurantCreateRequest request) {

		// 음식 카테고리 요청 검증
		restaurantFoodCategoryValidator.validate(request.foodCategoryIds());

		// 음식 카테고리 정규화
		Set<Long> foodCategoryIdSet = request.foodCategoryIds() == null
			? Set.of()
			: request.foodCategoryIds().stream()
				.collect(Collectors.toUnmodifiableSet());

		// 위치 정보 검색
		GeocodingResult result = naverGeocodingClient.geocode(request.address());

		// 위치 정보 생성
		Coordinate coordinate = new Coordinate(result.longitude(), result.latitude());
		Point location = geometryFactory.createPoint(coordinate);

		// 음식점 생성
		Restaurant restaurant = Restaurant.create(request.name(), request.address(), location);
		restaurantRepository.save(restaurant);

		// 음식점 주소 정보 생성
		RestaurantAddress restaurantAddress = RestaurantAddress.create(
			restaurant,
			result.sido(),
			result.sigungu(),
			result.eupmyeondong(),
			result.postalCode());
		restaurantAddressRepository.save(restaurantAddress);

		// 음식 카테고리 추가
		List<FoodCategory> categories = foodCategoryRepository.findAllById(foodCategoryIdSet);
		List<RestaurantFoodCategory> mappings = categories.stream()
			.map(category -> RestaurantFoodCategory.create(restaurant, category))
			.toList();
		restaurantFoodCategoryRepository.saveAll(mappings);

		// 음식점 이미지 추가
		saveImagesIfPresent(restaurant, request.imageIds());

		// 음식점 주간 스케줄 추가
		saveWeeklySchedulesIfPresent(restaurant, request.weeklySchedules());

		// 음식점 생성 이벤트 발행
		eventPublisher.publishRestaurantCreated(restaurant.getId());

		return new RestaurantCreateResponse(restaurant.getId(), restaurant.getCreatedAt());
	}

	@Transactional
	public RestaurantUpdateResponse updateRestaurant(long restaurantId, RestaurantUpdateRequest request) {

		Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		// 음식점 이름 교체
		if (request.name() != null && !request.name().equals(restaurant.getName())) {
			restaurant.changeName(request.name());
		}

		// 음식 카테고리 요청 검증
		restaurantFoodCategoryValidator.validate(request.foodCategoryIds());

		// 음식 카테고리 정규화
		Set<Long> foodCategoryIdSet = request.foodCategoryIds() == null
			? Set.of()
			: request.foodCategoryIds().stream()
				.collect(Collectors.toUnmodifiableSet());

		// 음식 카테고리 교체
		restaurantFoodCategoryRepository.deleteById(restaurantId);

		List<FoodCategory> categories = foodCategoryRepository.findAllById(foodCategoryIdSet);
		List<RestaurantFoodCategory> mappings = categories.stream()
			.map(category -> RestaurantFoodCategory.create(restaurant, category))
			.toList();
		restaurantFoodCategoryRepository.saveAll(mappings);

		// 음식점 이미지 교체
		List<Long> imageIds = restaurantImageRepository.findRestaurantImages(List.of(restaurantId)).stream()
			.map(RestaurantImageProjection::getImageId)
			.toList();
		restaurantImageRepository.deleteById(restaurantId);
		imageRepository.deleteAllByIdInBatch(imageIds);

		saveImagesIfPresent(restaurant, request.imageIds());

		return new RestaurantUpdateResponse(
			restaurant.getId(),
			restaurant.getCreatedAt(),
			restaurant.getUpdatedAt());
	}

	@Transactional
	public void deleteRestaurant(long restaurantId) {
		Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
		if (restaurant.isEmpty()) {
			return;
		}
		Restaurant entity = restaurant.get();
		if (entity.getDeletedAt() != null) {
			return;
		}

		// 음식점 소프트 삭제
		Instant now = Instant.now();
		entity.softDelete(now);

		// 음식점 이미지 소프트 삭제
		List<RestaurantImage> images = restaurantImageRepository.findByRestaurantIdAndDeletedAtIsNull(restaurantId);
		for (RestaurantImage image : images) {
			image.softDelete(now);
		}
	}

	private void saveImagesIfPresent(Restaurant restaurant, List<UUID> imageIds) {
		if (imageIds == null || imageIds.isEmpty()) {
			return;
		}

		List<RestaurantImage> images = new ArrayList<>();
		for (int index = 0; index < imageIds.size(); index++) {
			final int finalIndex = index;

			UUID imageId = imageIds.get(index);
			imageRepository.findByFileUuid(imageId)
				.ifPresent(
					image -> images.add(RestaurantImage.create(restaurant, image.getStorageKey(), finalIndex + 1)));
		}
		restaurantImageRepository.saveAll(images);
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

	private GroupRestaurantSearchCondition toGroupRestaurantSearchCondition(
		long groupId,
		NearbyRestaurantQueryParams q,
		RestaurantCursor restaurantCursor) {
		double latitude = q.latitude();
		double longitude = q.longitude();

		// 반경 기본값
		Integer radiusMeter = q.radius();
		if (radiusMeter == null ||
			radiusMeter > RestaurantSearchPolicy.MAX_RADIUS_METER) {
			radiusMeter = RestaurantSearchPolicy.DEFAULT_RADIUS_METER;
		}

		// 음식 카테고리 이름 정규화
		Set<String> foodCategories = q.categories() == null
			? Set.of()
			: q.categories().stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toUnmodifiableSet());

		// 페이지 크기 기본값
		Integer pageSize = q.size();
		if (pageSize == null ||
			pageSize > RestaurantSearchPolicy.MAX_PAGE_SIZE) {
			pageSize = RestaurantSearchPolicy.DEFAULT_PAGE_SIZE;
		}

		return new GroupRestaurantSearchCondition(
			groupId,
			latitude,
			longitude,
			radiusMeter,
			foodCategories,
			restaurantCursor,
			pageSize);
	}
}
