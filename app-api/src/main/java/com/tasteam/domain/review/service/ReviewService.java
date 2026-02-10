package com.tasteam.domain.review.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.service.DomainImageLinker;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.dto.response.ReviewSummaryResponse;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.dto.ReviewCursor;
import com.tasteam.domain.review.dto.ReviewDetailQueryDto;
import com.tasteam.domain.review.dto.ReviewMemberQueryDto;
import com.tasteam.domain.review.dto.ReviewQueryDto;
import com.tasteam.domain.review.dto.request.ReviewCreateRequest;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.domain.review.dto.response.ReviewDetailResponse;
import com.tasteam.domain.review.dto.response.ReviewKeywordItemResponse;
import com.tasteam.domain.review.entity.Keyword;
import com.tasteam.domain.review.entity.KeywordType;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.entity.ReviewKeyword;
import com.tasteam.domain.review.repository.KeywordRepository;
import com.tasteam.domain.review.repository.ReviewKeywordRepository;
import com.tasteam.domain.review.repository.ReviewQueryRepository;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.domain.review.repository.projection.ReviewKeywordProjection;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.type.SubgroupStatus;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.global.exception.code.ReviewErrorCode;
import com.tasteam.global.exception.code.SubgroupErrorCode;
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.global.utils.CursorPageBuilder;
import com.tasteam.global.utils.PaginationParamUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewService {

	private final RestaurantRepository restaurantRepository;
	private final GroupRepository groupRepository;
	private final SubgroupRepository subgroupRepository;
	private final MemberRepository memberRepository;
	private final ReviewRepository reviewRepository;
	private final KeywordRepository keywordRepository;
	private final ReviewQueryRepository reviewQueryRepository;
	private final ReviewKeywordRepository reviewKeywordRepository;
	private final DomainImageRepository domainImageRepository;
	private final FileService fileService;
	private final CursorCodec cursorCodec;
	private final DomainImageLinker domainImageLinker;

	@Transactional(readOnly = true)
	public List<ReviewKeywordItemResponse> getReviewKeywords(KeywordType type) {
		List<Keyword> keywords = (type == null)
			? keywordRepository.findAllByOrderByIdAsc()
			: keywordRepository.findByTypeOrderByIdAsc(type);
		return keywords.stream()
			.map(ReviewKeywordItemResponse::from)
			.toList();
	}

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

		Map<Long, List<DomainImageItem>> reviewImages = fileService.getDomainImageUrls(
			DomainType.REVIEW,
			List.of(reviewId));

		List<ReviewDetailResponse.ReviewImageResponse> images = reviewImages
			.getOrDefault(reviewId, List.of())
			.stream()
			.map(image -> new ReviewDetailResponse.ReviewImageResponse(image.imageId(), image.url()))
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
			domainImageLinker.linkImages(DomainType.REVIEW, review.getId(), request.imageIds());
		}

		return new ReviewCreateResponse(
			review.getId(),
			review.getCreatedAt());
	}

	@Transactional
	public void deleteReview(long memberId, long reviewId) {
		Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
			.orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));
		if (!review.getMember().getId().equals(memberId)) {
			throw new BusinessException(CommonErrorCode.NO_PERMISSION);
		}

		Instant now = Instant.now();
		review.softDelete(now);
		reviewKeywordRepository.deleteByReview_Id(reviewId);
		domainImageRepository.deleteAllByDomainTypeAndDomainId(DomainType.REVIEW, reviewId);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<ReviewResponse> getRestaurantReviews(
		long restaurantId,
		RestaurantReviewListRequest request) {
		// 조회 조건 검증
		if (!restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)) {
			throw new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND);
		}

		int resolvedSize = PaginationParamUtils.resolveSize(request.size());
		CursorPageBuilder<ReviewCursor> pageBuilder = buildCursorPageBuilderOrThrow(request.cursor());

		// 리뷰 목록
		List<ReviewQueryDto> reviewList = reviewQueryRepository.findRestaurantReviews(
			restaurantId,
			pageBuilder.cursor(),
			CursorPageBuilder.fetchSize(resolvedSize));

		return buildReviewPage(pageBuilder, reviewList, resolvedSize);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<ReviewResponse> getGroupReviews(
		long groupId,
		RestaurantReviewListRequest request) {
		groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		int resolvedSize = PaginationParamUtils.resolveSize(request.size());
		CursorPageBuilder<ReviewCursor> pageBuilder = buildCursorPageBuilderOrThrow(request.cursor());

		List<ReviewQueryDto> reviewList = reviewQueryRepository.findGroupReviews(
			groupId,
			pageBuilder.cursor(),
			CursorPageBuilder.fetchSize(resolvedSize));

		return buildReviewPage(pageBuilder, reviewList, resolvedSize);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<ReviewResponse> getSubgroupReviews(
		long subgroupId,
		RestaurantReviewListRequest request) {
		if (!subgroupRepository.existsByIdAndStatus(subgroupId, SubgroupStatus.ACTIVE)) {
			throw new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND);
		}

		int resolvedSize = PaginationParamUtils.resolveSize(request.size());
		CursorPageBuilder<ReviewCursor> pageBuilder = buildCursorPageBuilderOrThrow(request.cursor());

		List<ReviewQueryDto> reviewList = reviewQueryRepository.findSubgroupReviews(
			subgroupId,
			pageBuilder.cursor(),
			CursorPageBuilder.fetchSize(resolvedSize));

		return buildReviewPage(pageBuilder, reviewList, resolvedSize);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<ReviewSummaryResponse> getMemberReviews(
		long memberId,
		RestaurantReviewListRequest request) {
		memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		int resolvedSize = PaginationParamUtils.resolveSize(request.size());
		CursorPageBuilder<ReviewCursor> pageBuilder = buildCursorPageBuilderOrThrow(request.cursor());

		List<ReviewMemberQueryDto> reviewList = reviewQueryRepository.findMemberReviews(
			memberId,
			pageBuilder.cursor(),
			CursorPageBuilder.fetchSize(resolvedSize));

		if (reviewList.isEmpty()) {
			return CursorPageResponse.empty();
		}

		return buildReviewSummaryPage(pageBuilder, reviewList, resolvedSize);
	}

	private CursorPageBuilder<ReviewCursor> buildCursorPageBuilderOrThrow(String rawCursor) {
		CursorPageBuilder<ReviewCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, rawCursor, ReviewCursor.class);
		if (pageBuilder.isInvalid()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return pageBuilder;
	}

	private CursorPageResponse<ReviewResponse> buildReviewPage(
		CursorPageBuilder<ReviewCursor> pageBuilder,
		List<ReviewQueryDto> reviewList,
		int resolvedSize) {
		CursorPageBuilder.Page<ReviewQueryDto> page = pageBuilder.build(
			reviewList,
			resolvedSize,
			last -> new ReviewCursor(last.createdAt(), last.reviewId()));

		if (page.items().isEmpty()) {
			return CursorPageResponse.empty();
		}

		List<Long> reviewIds = extractReviewIds(page.items());
		Map<Long, List<String>> reviewKeywords = loadReviewKeywords(reviewIds);
		Map<Long, List<DomainImageItem>> reviewThumbnails = fileService.getDomainImageUrls(
			DomainType.REVIEW,
			reviewIds);
		Map<Long, String> restaurantIdToFirstImageUrl = resolveRestaurantFirstImageUrls(page.items());

		List<ReviewResponse> items = buildReviewResponses(
			page.items(), reviewKeywords, reviewThumbnails, restaurantIdToFirstImageUrl);

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				page.nextCursor(),
				page.hasNext(),
				items.size()));
	}

	private List<Long> extractReviewIds(List<ReviewQueryDto> pageContent) {
		return pageContent.stream()
			.map(ReviewQueryDto::reviewId)
			.toList();
	}

	private Map<Long, String> resolveRestaurantFirstImageUrls(List<ReviewQueryDto> pageContent) {
		List<Long> restaurantIds = pageContent.stream()
			.map(ReviewQueryDto::restaurantId)
			.filter(id -> id != null)
			.distinct()
			.toList();
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<DomainImageItem>> byRestaurant = fileService.getDomainImageUrls(DomainType.RESTAURANT,
			restaurantIds);
		return byRestaurant.entrySet().stream()
			.filter(e -> e.getValue() != null && !e.getValue().isEmpty())
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFirst().url()));
	}

	private List<ReviewResponse> buildReviewResponses(
		List<ReviewQueryDto> pageContent,
		Map<Long, List<String>> reviewKeywords,
		Map<Long, List<DomainImageItem>> reviewThumbnails,
		Map<Long, String> restaurantIdToFirstImageUrl) {
		return pageContent.stream()
			.map(review -> new ReviewResponse(
				review.reviewId(),
				review.groupId(),
				review.subgroupId(),
				review.groupName(),
				review.subgroupName(),
				new ReviewResponse.AuthorResponse(review.memberName()),
				review.content(),
				review.isRecommended(),
				reviewKeywords.getOrDefault(review.reviewId(), List.of()),
				convertToReviewImages(reviewThumbnails.get(review.reviewId())),
				review.createdAt(),
				review.restaurantId(),
				review.restaurantName(),
				review.groupLogoImageUrl(),
				review.groupAddress(),
				review.restaurantId() != null ? restaurantIdToFirstImageUrl.get(review.restaurantId()) : null,
				review.restaurantAddress()))
			.toList();
	}

	private CursorPageResponse<ReviewSummaryResponse> buildReviewSummaryPage(
		CursorPageBuilder<ReviewCursor> pageBuilder,
		List<ReviewMemberQueryDto> reviewList,
		int resolvedSize) {
		CursorPageBuilder.Page<ReviewMemberQueryDto> page = pageBuilder.build(
			reviewList,
			resolvedSize,
			last -> new ReviewCursor(last.createdAt(), last.reviewId()));

		List<ReviewSummaryResponse> items = buildReviewSummaryResponses(page.items());

		return new CursorPageResponse<>(
			items,
			buildMemberPagination(page.nextCursor(), page.hasNext(), items.size()));
	}

	private List<ReviewSummaryResponse> buildReviewSummaryResponses(List<ReviewMemberQueryDto> pageContent) {
		return pageContent.stream()
			.map(review -> new ReviewSummaryResponse(
				review.reviewId(),
				review.restaurantName(),
				review.restaurantAddress(),
				review.content()))
			.toList();
	}

	private CursorPageResponse.Pagination buildMemberPagination(String nextCursor, boolean hasNext, int size) {
		return new CursorPageResponse.Pagination(nextCursor, hasNext, size);
	}

	private Map<Long, List<String>> loadReviewKeywords(List<Long> reviewIds) {
		return reviewKeywordRepository
			.findReviewKeywords(reviewIds)
			.stream()
			.collect(Collectors.groupingBy(
				ReviewKeywordProjection::getReviewId,
				Collectors.mapping(ReviewKeywordProjection::getKeywordName, Collectors.toList())));
	}

	private List<ReviewResponse.ReviewImageResponse> convertToReviewImages(List<DomainImageItem> images) {
		if (images == null || images.isEmpty()) {
			return List.of();
		}
		return images.stream()
			.map(image -> new ReviewResponse.ReviewImageResponse(image.imageId(), image.url()))
			.toList();
	}
}
