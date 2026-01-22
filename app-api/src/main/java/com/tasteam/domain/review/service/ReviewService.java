package com.tasteam.domain.review.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.dto.response.MemberPreviewResponse;
import com.tasteam.domain.member.dto.response.ReviewPreviewResponse;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.support.CursorCodec;
import com.tasteam.domain.review.dto.ReviewCursor;
import com.tasteam.domain.review.dto.ReviewMemberQueryDto;
import com.tasteam.domain.review.dto.ReviewQueryDto;
import com.tasteam.domain.review.repository.ReviewImageRepository;
import com.tasteam.domain.review.repository.ReviewKeywordRepository;
import com.tasteam.domain.review.repository.ReviewQueryRepository;
import com.tasteam.domain.review.repository.projection.ReviewImageProjection;
import com.tasteam.domain.review.repository.projection.ReviewKeywordProjection;
import com.tasteam.global.dto.api.PaginationResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.global.utils.CursorCodec;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 100;

	private final RestaurantRepository restaurantRepository;
	private final GroupRepository groupRepository;
	private final MemberRepository memberRepository;
	private final ReviewQueryRepository reviewQueryRepository;
	private final ReviewKeywordRepository reviewKeywordRepository;
	private final ReviewImageRepository reviewImageRepository;
	private final CursorCodec cursorCodec;

	public CursorPageResponse<ReviewResponse> getRestaurantReviews(
		long restaurantId,
		RestaurantReviewListRequest request) {
		// 조회 조건 검증
		if (!restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)) {
			throw new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND);
		}

		int resolvedSize = resolveSize(request.size());
		ReviewCursor cursor = parseCursor(request.cursor());

		// 리뷰 목록
		List<ReviewQueryDto> reviewList = reviewQueryRepository.findRestaurantReviews(
			restaurantId,
			cursor,
			resolvedSize + 1);

		if (reviewList.isEmpty()) {
			return CursorPageResponse.empty();
		}

		// 다음 커서 생성
		boolean hasNext = reviewList.size() > resolvedSize;
		List<ReviewQueryDto> pageContent = hasNext ? reviewList.subList(0, resolvedSize) : reviewList;

		String nextCursor = null;
		if (hasNext) {
			ReviewQueryDto last = pageContent.get(pageContent.size() - 1);
			nextCursor = cursorCodec.encode(new ReviewCursor(last.createdAt(), last.reviewId()));
		}

		// 리뷰 아이디 목록
		List<Long> reviewIds = pageContent.stream()
			.map(ReviewQueryDto::reviewId)
			.toList();

		// 리뷰 키워드 목록
		Map<Long, List<String>> reviewKeywords = reviewKeywordRepository
			.findReviewKeywords(reviewIds)
			.stream()
			.collect(Collectors.groupingBy(
				ReviewKeywordProjection::getReviewId,
				Collectors.mapping(ReviewKeywordProjection::getKeywordName, Collectors.toList())));

		// 리뷰 대표 이미지 (최대 1장)
		Map<Long, ReviewResponse.ReviewImageResponse> reviewThumbnails = reviewImageRepository
			.findReviewImages(reviewIds)
			.stream()
			.collect(Collectors.groupingBy(
				ReviewImageProjection::getReviewId,
				Collectors.collectingAndThen(
					Collectors.toList(),
					images -> {
						ReviewImageProjection first = images.get(0);
						return new ReviewResponse.ReviewImageResponse(first.getImageId(), first.getImageUrl());
					})));

		// 리뷰 관련 정보 조립
		List<ReviewResponse> items = pageContent.stream()
			.map(review -> new ReviewResponse(
				review.reviewId(),
				new ReviewResponse.AuthorResponse(review.memberName()),
				review.content(),
				review.isRecommended(),
				reviewKeywords.getOrDefault(review.reviewId(), List.of()),
				reviewThumbnails.get(review.reviewId()),
				review.createdAt()))
			.toList();

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				nextCursor,
				hasNext,
				items.size()));
	}

	public CursorPageResponse<ReviewResponse> getGroupReviews(
		long groupId,
		RestaurantReviewListRequest request) {
		groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		int resolvedSize = resolveSize(request.size());
		ReviewCursor parsedCursor = parseCursor(request.cursor());

		List<ReviewQueryDto> reviewList = reviewQueryRepository.findGroupReviews(
			groupId,
			parsedCursor,
			resolvedSize + 1);

		if (reviewList.isEmpty()) {
			return CursorPageResponse.empty();
		}

		boolean hasNext = reviewList.size() > resolvedSize;
		List<ReviewQueryDto> pageContent = hasNext ? reviewList.subList(0, resolvedSize) : reviewList;

		String nextCursor = null;
		if (hasNext) {
			ReviewQueryDto last = pageContent.get(pageContent.size() - 1);
			nextCursor = cursorCodec.encode(new ReviewCursor(last.createdAt(), last.reviewId()));
		}

		List<Long> reviewIds = pageContent.stream()
			.map(ReviewQueryDto::reviewId)
			.toList();

		Map<Long, List<String>> reviewKeywords = reviewKeywordRepository
			.findReviewKeywords(reviewIds)
			.stream()
			.collect(Collectors.groupingBy(
				ReviewKeywordProjection::getReviewId,
				Collectors.mapping(ReviewKeywordProjection::getKeywordName, Collectors.toList())));

		Map<Long, ReviewResponse.ReviewImageResponse> reviewThumbnails = reviewImageRepository
			.findReviewImages(reviewIds)
			.stream()
			.collect(Collectors.groupingBy(
				ReviewImageProjection::getReviewId,
				Collectors.collectingAndThen(
					Collectors.toList(),
					images -> {
						ReviewImageProjection first = images.get(0);
						return new ReviewResponse.ReviewImageResponse(first.getImageId(), first.getImageUrl());
					})));

		List<ReviewResponse> items = pageContent.stream()
			.map(review -> new ReviewResponse(
				review.reviewId(),
				new ReviewResponse.AuthorResponse(review.memberName()),
				review.content(),
				review.isRecommended(),
				reviewKeywords.getOrDefault(review.reviewId(), List.of()),
				reviewThumbnails.get(review.reviewId()),
				review.createdAt()))
			.toList();

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				nextCursor,
				hasNext,
				items.size()));
	}

	private int resolveSize(Integer size) {
		if (size == null) {
			return DEFAULT_PAGE_SIZE;
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return size;
	}

	private ReviewCursor parseCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		return cursorCodec.decode(cursor, ReviewCursor.class);
	}

	public MemberPreviewResponse<ReviewPreviewResponse> getMemberReviews(
		long memberId,
		RestaurantReviewListRequest request) {
		memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		ReviewCursor parsedCursor = parseCursor(request.cursor());
		int resolvedSize = resolveSize(request.size());

		List<ReviewMemberQueryDto> reviewList = reviewQueryRepository.findMemberReviews(
			memberId,
			parsedCursor,
			resolvedSize + 1);

		boolean hasNext = reviewList.size() > resolvedSize;
		List<ReviewMemberQueryDto> pageContent = hasNext ? reviewList.subList(0, resolvedSize) : reviewList;

		if (pageContent.isEmpty()) {
			return MemberPreviewResponse.empty();
		}

		String nextCursor = null;
		if (hasNext) {
			ReviewMemberQueryDto last = pageContent.get(pageContent.size() - 1);
			nextCursor = cursorCodec.encode(new ReviewCursor(last.createdAt(), last.reviewId()));
		}

		List<ReviewPreviewResponse> items = pageContent.stream()
			.map(review -> new ReviewPreviewResponse(
				review.reviewId(),
				review.restaurantName(),
				review.restaurantAddress(),
				review.content()))
			.toList();

		PaginationResponse page = PaginationResponse.builder()
			.nextCursor(nextCursor)
			.size(items.size())
			.hasNext(hasNext)
			.build();

		return new MemberPreviewResponse<>(items, page);
	}
}
