package com.tasteam.domain.review.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
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
import com.tasteam.global.exception.code.FileErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.global.exception.code.ReviewErrorCode;
import com.tasteam.global.exception.code.SubgroupErrorCode;
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.infra.storage.StorageProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 100;

	private final RestaurantRepository restaurantRepository;
	private final GroupRepository groupRepository;
	private final SubgroupRepository subgroupRepository;
	private final MemberRepository memberRepository;
	private final ReviewRepository reviewRepository;
	private final KeywordRepository keywordRepository;
	private final ReviewQueryRepository reviewQueryRepository;
	private final ReviewKeywordRepository reviewKeywordRepository;
	private final DomainImageRepository domainImageRepository;
	private final ImageRepository imageRepository;
	private final StorageProperties storageProperties;
	private final CursorCodec cursorCodec;

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

		List<ReviewDetailResponse.ReviewImageResponse> images = domainImageRepository
			.findAllByDomainTypeAndDomainIdIn(DomainType.REVIEW, List.of(reviewId))
			.stream()
			.map(di -> new ReviewDetailResponse.ReviewImageResponse(
				di.getImage().getId(),
				buildPublicUrl(di.getImage().getStorageKey())))
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
			for (int index = 0; index < request.imageIds().size(); index++) {
				UUID fileUuid = request.imageIds().get(index);
				int sortOrder = index;
				Image image = imageRepository.findByFileUuid(fileUuid)
					.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
				if (image.getStatus() != ImageStatus.PENDING) {
					throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
				}
				image.activate();
				DomainImage domainImage = DomainImage.create(DomainType.REVIEW, review.getId(), image, sortOrder);
				domainImageRepository.save(domainImage);
			}
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

		int resolvedSize = resolveSize(request.size());
		ReviewCursor cursor = parseCursor(request.cursor());

		// 리뷰 목록
		List<ReviewQueryDto> reviewList = reviewQueryRepository.findRestaurantReviews(
			restaurantId,
			cursor,
			resolvedSize + 1);

		return buildReviewPage(reviewList, resolvedSize);
	}

	@Transactional(readOnly = true)
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

		return buildReviewPage(reviewList, resolvedSize);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<ReviewResponse> getSubgroupReviews(
		long subgroupId,
		RestaurantReviewListRequest request) {
		if (!subgroupRepository.existsByIdAndStatus(subgroupId, SubgroupStatus.ACTIVE)) {
			throw new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND);
		}

		int resolvedSize = resolveSize(request.size());
		ReviewCursor parsedCursor = parseCursor(request.cursor());

		List<ReviewQueryDto> reviewList = reviewQueryRepository.findSubgroupReviews(
			subgroupId,
			parsedCursor,
			resolvedSize + 1);

		return buildReviewPage(reviewList, resolvedSize);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<ReviewSummaryResponse> getMemberReviews(
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

		if (reviewList.isEmpty()) {
			return CursorPageResponse.empty();
		}

		return buildReviewSummaryPage(reviewList, resolvedSize);
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

	private CursorPageResponse<ReviewResponse> buildReviewPage(
		List<ReviewQueryDto> reviewList,
		int resolvedSize) {
		if (reviewList.isEmpty()) {
			return CursorPageResponse.empty();
		}

		boolean hasNext = reviewList.size() > resolvedSize;
		List<ReviewQueryDto> pageContent = sliceContent(reviewList, resolvedSize);
		String nextCursor = buildNextCursor(pageContent, hasNext);

		List<Long> reviewIds = extractReviewIds(pageContent);
		Map<Long, List<String>> reviewKeywords = loadReviewKeywords(reviewIds);
		Map<Long, ReviewResponse.ReviewImageResponse> reviewThumbnails = loadReviewThumbnails(reviewIds);

		List<ReviewResponse> items = buildReviewResponses(pageContent, reviewKeywords, reviewThumbnails);

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				nextCursor,
				hasNext,
				items.size()));
	}

	private List<ReviewQueryDto> sliceContent(List<ReviewQueryDto> reviewList, int resolvedSize) {
		boolean hasNext = reviewList.size() > resolvedSize;
		return hasNext ? reviewList.subList(0, resolvedSize) : reviewList;
	}

	private String buildNextCursor(List<ReviewQueryDto> pageContent, boolean hasNext) {
		if (!hasNext) {
			return null;
		}
		ReviewQueryDto last = pageContent.get(pageContent.size() - 1);
		return cursorCodec.encode(new ReviewCursor(last.createdAt(), last.reviewId()));
	}

	private List<Long> extractReviewIds(List<ReviewQueryDto> pageContent) {
		return pageContent.stream()
			.map(ReviewQueryDto::reviewId)
			.toList();
	}

	private List<ReviewResponse> buildReviewResponses(
		List<ReviewQueryDto> pageContent,
		Map<Long, List<String>> reviewKeywords,
		Map<Long, ReviewResponse.ReviewImageResponse> reviewThumbnails) {
		return pageContent.stream()
			.map(review -> new ReviewResponse(
				review.reviewId(),
				new ReviewResponse.AuthorResponse(review.memberName()),
				review.content(),
				review.isRecommended(),
				reviewKeywords.getOrDefault(review.reviewId(), List.of()),
				reviewThumbnails.get(review.reviewId()),
				review.createdAt()))
			.toList();
	}

	private CursorPageResponse<ReviewSummaryResponse> buildReviewSummaryPage(
		List<ReviewMemberQueryDto> reviewList,
		int resolvedSize) {
		boolean hasNext = reviewList.size() > resolvedSize;
		List<ReviewMemberQueryDto> pageContent = sliceMemberContent(reviewList, resolvedSize);
		String nextCursor = buildMemberNextCursor(pageContent, hasNext);

		List<ReviewSummaryResponse> items = buildReviewSummaryResponses(pageContent);

		return new CursorPageResponse<>(
			items,
			buildMemberPagination(nextCursor, hasNext, items.size()));
	}

	private List<ReviewMemberQueryDto> sliceMemberContent(
		List<ReviewMemberQueryDto> reviewList,
		int resolvedSize) {
		boolean hasNext = reviewList.size() > resolvedSize;
		return hasNext ? reviewList.subList(0, resolvedSize) : reviewList;
	}

	private String buildMemberNextCursor(List<ReviewMemberQueryDto> pageContent, boolean hasNext) {
		if (!hasNext) {
			return null;
		}
		ReviewMemberQueryDto last = pageContent.get(pageContent.size() - 1);
		return cursorCodec.encode(new ReviewCursor(last.createdAt(), last.reviewId()));
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

	private Map<Long, ReviewResponse.ReviewImageResponse> loadReviewThumbnails(List<Long> reviewIds) {
		return domainImageRepository
			.findAllByDomainTypeAndDomainIdIn(DomainType.REVIEW, reviewIds)
			.stream()
			.collect(Collectors.groupingBy(
				DomainImage::getDomainId,
				Collectors.collectingAndThen(
					Collectors.toList(),
					images -> {
						DomainImage first = images.get(0);
						return new ReviewResponse.ReviewImageResponse(
							first.getImage().getId(),
							buildPublicUrl(first.getImage().getStorageKey()));
					})));
	}

	private String buildPublicUrl(String storageKey) {
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
