package com.tasteam.domain.restaurant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tasteam.domain.restaurant.controller.docs.RestaurantControllerDocs;
import com.tasteam.domain.restaurant.dto.request.RestaurantCreateRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantUpdateRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.*;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.dto.request.ReviewCreateRequest;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController implements RestaurantControllerDocs {

	private final RestaurantService restaurantService;
	private final ReviewService reviewService;

	/**
	 * 프론트 연동 테스트용 mock 목록 API.
	 * 실제 쿼리 파라미터가 없어도 동작하도록 모두 optional로 받습니다.
	 */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public SuccessResponse<CursorPageResponse<RestaurantListItem>> getRestaurantsMock(
		@RequestParam(required = false)
		Double latitude,
		@RequestParam(required = false)
		Double longitude,
		@RequestParam(required = false)
		Double lat,
		@RequestParam(required = false)
		Double lng,
		@RequestParam(required = false)
		Integer size,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		String category) {

		List<RestaurantListItem> items = List.of(
			new RestaurantListItem(
				6001L,
				"로컬 맛집",
				"서울특별시 마포구 합정동 123-45",
				120.0,
				List.of("한식"),
				List.of(new RestaurantImageDto(6001L, "https://picsum.photos/seed/tasteam-6001/640/480"))),
			new RestaurantListItem(
				6002L,
				"로컬 카페",
				"서울특별시 강남구 역삼동 222-11",
				340.0,
				List.of("카페"),
				List.of(new RestaurantImageDto(6002L, "https://picsum.photos/seed/tasteam-6002/640/480"))),
			new RestaurantListItem(
				8001L,
				"로컬 양식당",
				"서울특별시 종로구 세종대로 1",
				560.0,
				List.of("양식"),
				List.of(new RestaurantImageDto(8001L, "https://picsum.photos/seed/tasteam-8001/640/480"))),
			new RestaurantListItem(
				8002L,
				"로컬 분식집",
				"서울특별시 중구 명동 2",
				780.0,
				List.of("분식"),
				List.of(new RestaurantImageDto(8002L, "https://picsum.photos/seed/tasteam-8002/640/480"))),
			new RestaurantListItem(
				8003L,
				"로컬 베이커리",
				"서울특별시 용산구 이태원 3",
				910.0,
				List.of("베이커리"),
				List.of(new RestaurantImageDto(8003L, "https://picsum.photos/seed/tasteam-8003/640/480"))),
			new RestaurantListItem(
				8004L,
				"로컬 치킨",
				"서울특별시 성동구 왕십리 4",
				1040.0,
				List.of("치킨"),
				List.of(new RestaurantImageDto(8004L, "https://picsum.photos/seed/tasteam-8004/640/480"))),
			new RestaurantListItem(
				8005L,
				"로컬 피자",
				"서울특별시 광진구 건대입구 5",
				1270.0,
				List.of("피자"),
				List.of(new RestaurantImageDto(8005L, "https://picsum.photos/seed/tasteam-8005/640/480"))),
			new RestaurantListItem(
				8006L,
				"로컬 아시아",
				"서울특별시 마포구 홍대입구 6",
				1490.0,
				List.of("아시아"),
				List.of(new RestaurantImageDto(8006L, "https://picsum.photos/seed/tasteam-8006/640/480"))),
			new RestaurantListItem(
				8007L,
				"로컬 한식",
				"서울특별시 영등포구 여의도 7",
				1720.0,
				List.of("한식"),
				List.of(new RestaurantImageDto(8007L, "https://picsum.photos/seed/tasteam-8007/640/480"))),
			new RestaurantListItem(
				8008L,
				"로컬 일식",
				"서울특별시 서초구 서초동 8",
				1960.0,
				List.of("일식"),
				List.of(new RestaurantImageDto(8008L, "https://picsum.photos/seed/tasteam-8008/640/480"))),
			new RestaurantListItem(
				8009L,
				"로컬 중식",
				"서울특별시 강서구 마곡 9",
				2210.0,
				List.of("중식"),
				List.of(new RestaurantImageDto(8009L, "https://picsum.photos/seed/tasteam-8009/640/480"))),
			new RestaurantListItem(
				8010L,
				"로컬 카페 2",
				"서울특별시 송파구 잠실 10",
				2480.0,
				List.of("카페"),
				List.of(new RestaurantImageDto(8010L, "https://picsum.photos/seed/tasteam-8010/640/480"))));

		int pageSize = (size == null || size < 1) ? items.size() : Math.min(size, items.size());
		List<RestaurantListItem> pageItems = items.subList(0, pageSize);

		CursorPageResponse<RestaurantListItem> response = new CursorPageResponse<>(
			pageItems,
			new CursorPageResponse.Pagination(
				null,
				pageSize < items.size(),
				pageSize));

		return SuccessResponse.success(response);
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}")
	public SuccessResponse<RestaurantDetailResponse> getRestaurant(
		@PathVariable
		Long restaurantId) {
		return SuccessResponse.success(restaurantService.getRestaurantDetail(restaurantId));
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public SuccessResponse<RestaurantCreateResponse> createRestaurant(@RequestBody
	RestaurantCreateRequest request) {
		return SuccessResponse.success(restaurantService.createRestaurant(request));
	}

	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/{restaurantId}")
	public SuccessResponse<RestaurantUpdateResponse> updateRestaurant(
		@PathVariable
		Long restaurantId,
		@RequestBody
		RestaurantUpdateRequest request) {
		return SuccessResponse.success(restaurantService.updateRestaurant(restaurantId, request));
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{restaurantId}")
	public void deleteRestaurant(@PathVariable
	Long restaurantId) {
		restaurantService.deleteRestaurant(restaurantId);
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}/reviews")
	public SuccessResponse<CursorPageResponse<ReviewResponse>> getRestaurantReviews(
		@PathVariable
		Long restaurantId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(reviewService.getRestaurantReviews(restaurantId, request));
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('USER')")
	@PostMapping("/{restaurantId}/reviews")
	public SuccessResponse<ReviewCreateResponse> createReview(
		@PathVariable
		Long restaurantId,
		@CurrentUser
		Long memberId,
		@Valid @RequestBody
		ReviewCreateRequest request) {
		return SuccessResponse.success(reviewService.createReview(memberId, restaurantId, request));
	}
}
