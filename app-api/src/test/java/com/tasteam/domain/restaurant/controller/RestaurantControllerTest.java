package com.tasteam.domain.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.dto.response.RestaurantListItem;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.fixture.RestaurantRequestFixture;

@ControllerWebMvcTest(RestaurantController.class)
class RestaurantControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RestaurantService restaurantService;

	@MockitoBean
	private ReviewService reviewService;

	@Nested
	@DisplayName("음식점 목록 조회")
	class GetRestaurants {

		@Test
		@DisplayName("성공 응답은 고정 포맷을 따른다")
		void 음식점_목록_조회_성공_응답_포맷_검증() throws Exception {
			// given
			CursorPageResponse<RestaurantListItem> response = new CursorPageResponse<>(
				List.of(new RestaurantListItem(
					1L,
					"맛집식당",
					"서울시 강남구",
					500.0,
					List.of("한식"),
					List.of(new RestaurantImageDto(1L, "https://example.com/img.jpg")),
					"맛집으로 유명한 식당입니다.")),
				new CursorPageResponse.Pagination(null, false, 20));

			given(restaurantService.getRestaurants(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("latitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LAT))
				.param("longitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LNG)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.pagination").exists());
		}

		@Test
		@DisplayName("좌표로 음식점 목록을 조회하면 커서 페이징 결과를 반환한다")
		void 음식점_목록_조회_성공() throws Exception {
			// given
			CursorPageResponse<RestaurantListItem> response = new CursorPageResponse<>(
				List.of(new RestaurantListItem(
					1L,
					"맛집식당",
					"서울시 강남구",
					500.0,
					List.of("한식"),
					List.of(new RestaurantImageDto(1L, "https://example.com/img.jpg")),
					"맛집으로 유명한 식당입니다.")),
				new CursorPageResponse.Pagination(null, false, 20));

			given(restaurantService.getRestaurants(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("latitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LAT))
				.param("longitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LNG))
				.param("radius", String.valueOf(RestaurantRequestFixture.DEFAULT_RADIUS))
				.param("size", String.valueOf(RestaurantRequestFixture.DEFAULT_SIZE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].name").value("맛집식당"))
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}

		@Test
		@DisplayName("groupId 없이도 음식점 목록을 조회할 수 있다")
		void groupId_없이_조회_성공() throws Exception {
			// given
			CursorPageResponse<RestaurantListItem> response = new CursorPageResponse<>(
				List.of(new RestaurantListItem(
					1L,
					"맛집식당",
					"서울시 강남구",
					500.0,
					List.of("한식"),
					List.of(new RestaurantImageDto(1L, "https://example.com/img.jpg")),
					"맛집으로 유명한 식당입니다.")),
				new CursorPageResponse.Pagination(null, false, 20));

			given(restaurantService.getRestaurants(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("latitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LAT))
				.param("longitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LNG)))
				.andExpect(status().isOk());
		}

		@Test
		@DisplayName("위도가 없으면 400 에러를 반환한다")
		void 위도_누락시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("longitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LNG)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("경도가 없으면 400 에러를 반환한다")
		void 경도_누락시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("latitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LAT)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("위도 타입이 올바르지 않으면 400 에러를 반환한다")
		void 위도_타입_오류시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("latitude", "abc")
				.param("longitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LNG)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("좌표 범위를 벗어나면 400 에러를 반환한다")
		void 좌표_범위_검증_실패시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("latitude", "200")
				.param("longitude", "200"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("size가 0이면 400 에러를 반환한다")
		void size가_0이면_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("latitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LAT))
				.param("longitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LNG))
				.param("size", "0"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("size가 음수면 400 에러를 반환한다")
		void size가_음수면_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/restaurants")
				.param("latitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LAT))
				.param("longitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LNG))
				.param("size", "-1"))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("음식점 상세 조회")
	class GetRestaurant {

		@Test
		@DisplayName("성공 응답은 고정 포맷을 따른다")
		void 음식점_상세_조회_성공_응답_포맷_검증() throws Exception {
			// given
			RestaurantDetailResponse response = new RestaurantDetailResponse(
				1L, "맛집식당", "서울시 강남구", "02-1234-5678", List.of("한식"),
				List.of(), new RestaurantImageDto(1L, "https://example.com/img.jpg"),
				false,
				new RestaurantDetailResponse.RecommendStatResponse(10L, 2L, 83L),
				"AI 요약", "AI 특징",
				Instant.now(), Instant.now());

			given(restaurantService.getRestaurantDetail(1L)).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants/{restaurantId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").exists());
		}

		@Test
		@DisplayName("음식점 ID로 상세 정보를 조회하면 음식점 정보를 반환한다")
		void 음식점_상세_조회_성공() throws Exception {
			// given
			RestaurantDetailResponse response = new RestaurantDetailResponse(
				1L, "맛집식당", "서울시 강남구", "02-1234-5678", List.of("한식"),
				List.of(), new RestaurantImageDto(1L, "https://example.com/img.jpg"),
				false,
				new RestaurantDetailResponse.RecommendStatResponse(10L, 2L, 83L),
				"AI 요약", "AI 특징",
				Instant.now(), Instant.now());

			given(restaurantService.getRestaurantDetail(1L)).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants/{restaurantId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.name").value("맛집식당"))
				.andExpect(jsonPath("$.data.recommendStat.recommendedCount").value(10));
		}

		@Test
		@DisplayName("음식점 ID 타입이 올바르지 않으면 400 에러를 반환한다")
		void 음식점_ID_타입_오류시_400_에러() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/restaurants/{restaurantId}", "abc"))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("음식점 리뷰 목록 조회")
	class GetRestaurantReviews {

		@Test
		@DisplayName("음식점 리뷰를 조회하면 커서 페이징 결과를 반환한다")
		void 음식점_리뷰_목록_조회_성공() throws Exception {
			// given
			CursorPageResponse<ReviewResponse> response = new CursorPageResponse<>(
				List.of(new ReviewResponse(1L, 2L, 3L, "테스트그룹", "테스트하위그룹",
					new ReviewResponse.AuthorResponse("테스트유저"),
					"맛있어요", true, List.of("친절", "깨끗"),
					List.of(new ReviewResponse.ReviewImageResponse(1L, "https://example.com/review.jpg")),
					Instant.now(), null, null, null, null, null, null)),
				new CursorPageResponse.Pagination(null, false, 20));

			given(reviewService.getRestaurantReviews(eq(1L), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/reviews", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].author.nickname").value("테스트유저"))
				.andExpect(jsonPath("$.data.items[0].contentPreview").value("맛있어요"));
		}

		@Test
		@DisplayName("size 없이도 음식점 리뷰 목록을 조회할 수 있다")
		void size_없이_리뷰_목록_조회_성공() throws Exception {
			// given
			CursorPageResponse<ReviewResponse> response = new CursorPageResponse<>(
				List.of(new ReviewResponse(1L, 2L, 3L, "테스트그룹", "테스트하위그룹",
					new ReviewResponse.AuthorResponse("테스트유저"),
					"맛있어요", true, List.of("친절", "깨끗"),
					List.of(new ReviewResponse.ReviewImageResponse(1L, "https://example.com/review.jpg")),
					Instant.now(), null, null, null, null, null, null)),
				new CursorPageResponse.Pagination(null, false, 20));

			given(reviewService.getRestaurantReviews(eq(1L), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/reviews", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("음식점 ID 타입이 올바르지 않으면 400 에러를 반환한다")
		void 음식점_리뷰_목록_조회시_음식점_ID_타입_오류_400() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/reviews", "abc"))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("음식점 리뷰 작성")
	class CreateReview {

		@Test
		@DisplayName("리뷰를 작성하면 201과 생성된 ID를 반환한다")
		void 리뷰_작성_성공() throws Exception {
			// given
			ReviewCreateResponse response = new ReviewCreateResponse(1L, Instant.now());
			given(reviewService.createReview(anyLong(), eq(1L), any())).willReturn(response);

			var request = new com.tasteam.domain.review.dto.request.ReviewCreateRequest(
				1L, null, "맛있어요", true, List.of(1L, 2L), null);

			// when & then
			mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/reviews", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1));
		}

		@Test
		@DisplayName("빈 요청 바디로 리뷰를 작성하면 400 에러를 반환한다")
		void 리뷰_작성시_빈_요청_바디_400() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/reviews", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("필수 필드가 누락되면 400 에러를 반환한다")
		void 리뷰_작성시_필수_필드_누락_400() throws Exception {
			// given
			var invalidRequest = new com.tasteam.domain.review.dto.request.ReviewCreateRequest(
				null, null, "맛있어요", null, null, null);

			// when & then
			mockMvc.perform(post("/api/v1/restaurants/{restaurantId}/reviews", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest());
		}
	}
}
