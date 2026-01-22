package com.tasteam.domain.review.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.repository.ImageRepository;
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
import com.tasteam.domain.review.dto.ReviewDetailQueryDto;
import com.tasteam.domain.review.dto.ReviewMemberQueryDto;
import com.tasteam.domain.review.dto.ReviewQueryDto;
import com.tasteam.domain.review.dto.request.ReviewCreateRequest;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.domain.review.dto.response.ReviewDetailResponse;
import com.tasteam.domain.review.entity.Keyword;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.entity.ReviewImage;
import com.tasteam.domain.review.entity.ReviewKeyword;
import com.tasteam.domain.review.repository.KeywordRepository;
import com.tasteam.domain.review.repository.ReviewImageRepository;
import com.tasteam.domain.review.repository.ReviewKeywordRepository;
import com.tasteam.domain.review.repository.ReviewQueryRepository;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.domain.review.repository.projection.ReviewImageProjection;
import com.tasteam.domain.review.repository.projection.ReviewKeywordProjection;
import com.tasteam.global.dto.api.PaginationResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.global.exception.code.ReviewErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 100;

	private final RestaurantRepository restaurantRepository;
	private final GroupRepository groupRepository;
	private final MemberRepository memberRepository;
	private final ReviewRepository reviewRepository;
	private final KeywordRepository keywordRepository;
	private final ReviewQueryRepository reviewQueryRepository;
	private final ReviewKeywordRepository reviewKeywordRepository;
	private final ReviewImageRepository reviewImageRepository;
	private final ImageRepository imageRepository;
	private final CursorCodec cursorCodec;

	public ReviewDetailResponse getReviewDetail(long reviewId) {
		ReviewDetailQueryDto review = reviewQueryRepository.findReviewDetail(reviewId);
		if (review == null) {
			throw new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND);
		}

		List<String> keywords = reviewKeywordRepository
			.findReviewKeywords(List.of(reviewId))
			.stream()
			.map(ReviewKeywordProjection::getKeywordName)
			.toList();

		List<ReviewDetailResponse.ReviewImageResponse> images = reviewImageRepository
			.findReviewImages(List.of(reviewId))
			.stream()
			.map(image -> new ReviewDetailResponse.ReviewImageResponse(
				image.getImageId(),
				image.getImageUrl()))
			.toList();

		return new ReviewDetailResponse(
			review.reviewId(),
			new ReviewDetailResponse.RestaurantResponse(
				review.restaurantId(),
				review.restaurantName()),
			new ReviewDetailResponse.AuthorResponse(
				review.memberId(),
				review.memberNickname()),
			review.content(),
			review.isRecommended(),
			keywords,
			images,
			review.createdAt(),
			review.updatedAt());
	}

	@Transactional
	public ReviewCreateResponse createReview(
		long memberId,
		long restaurantId,
		ReviewCreateRequest request) {
		if (!restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)) {
			throw new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND);
		}
		if (!memberRepository.existsByIdAndDeletedAtIsNull(memberId)) {
			throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
		}
		if (!groupRepository.existsByIdAndDeletedAtIsNull(request.groupId())) {
			throw new BusinessException(GroupErrorCode.GROUP_NOT_FOUND);
		}

		List<Keyword> keywords = keywordRepository.findAllById(request.keywordIds());
		if (keywords.size() != request.keywordIds().size()) {
			throw new BusinessException(ReviewErrorCode.KEYWORD_NOT_FOUND);
		}

		Review review = Review.create(
			restaurantRepository.getReferenceById(restaurantId),
			memberRepository.getReferenceById(memberId),
			request.groupId(),
			request.subgroupId(),
			request.content(),
			request.isRecommended());
		reviewRepository.save(review);

		List<ReviewKeyword> mappings = keywords.stream()
			.map(keyword -> ReviewKeyword.create(review, keyword))
			.toList();
		reviewKeywordRepository.saveAll(mappings);

		if (request.imageIds() != null && !request.imageIds().isEmpty()) {
			List<ReviewImage> images = new ArrayList<>();
			for (int index = 0; index < request.imageIds().size(); index++) {
				imageRepository.findByFileUuid(request.imageIds().get(index))
					// TODO: storage key -> url 변환 필요
					.ifPresent(image -> images.add(ReviewImage.create(review, image.getStorageKey())));
			}
			reviewImageRepository.saveAll(images);
		}

		// FIXME: 공통 응답 래퍼 분리
		return new ReviewCreateResponse(
			new ReviewCreateResponse.ReviewCreateData(
				review.getId(),
				review.getCreatedAt()));
	}

	@Transactional
	public void deleteReview(long memberId, long reviewId) {
		Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
			.orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));
		if (!review.getMember().getId().equals(memberId)) {
			throw new BusinessException(CommonErrorCode.NO_PERMISSION);
		}

		// 리뷰 키워드 물리 삭제
		Instant now = Instant.now();
		review.softDelete(now);
		reviewKeywordRepository.deleteByReview_Id(reviewId);

		// 리뷰 이미지 소프트 삭제
		List<ReviewImage> images = reviewImageRepository.findByReview_IdAndDeletedAtIsNull(reviewId);
		for (ReviewImage image : images) {
			image.softDelete(now);
		}
	}

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
}
