package com.tasteam.domain.favorite.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.favorite.dto.FavoriteCountBySubgroupDto;
import com.tasteam.domain.favorite.dto.FavoriteCursor;
import com.tasteam.domain.favorite.dto.FavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.dto.FavoriteSubgroupTargetRow;
import com.tasteam.domain.favorite.dto.SubgroupFavoriteCursor;
import com.tasteam.domain.favorite.dto.SubgroupFavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.dto.response.FavoriteCreateResponse;
import com.tasteam.domain.favorite.dto.response.FavoritePageTargetsResponse;
import com.tasteam.domain.favorite.dto.response.FavoriteRestaurantItem;
import com.tasteam.domain.favorite.dto.response.RestaurantFavoriteTargetsResponse;
import com.tasteam.domain.favorite.dto.response.SubgroupFavoriteRestaurantItem;
import com.tasteam.domain.favorite.entity.MemberFavoriteRestaurant;
import com.tasteam.domain.favorite.entity.SubgroupFavoriteRestaurant;
import com.tasteam.domain.favorite.repository.MemberFavoriteRestaurantQueryRepository;
import com.tasteam.domain.favorite.repository.MemberFavoriteRestaurantRepository;
import com.tasteam.domain.favorite.repository.SubgroupFavoriteRestaurantQueryRepository;
import com.tasteam.domain.favorite.repository.SubgroupFavoriteRestaurantRepository;
import com.tasteam.domain.favorite.type.FavoriteState;
import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.type.SubgroupStatus;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FavoriteErrorCode;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.global.exception.code.SubgroupErrorCode;
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.global.utils.CursorPageBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final MemberFavoriteRestaurantRepository memberFavoriteRestaurantRepository;
	private final MemberFavoriteRestaurantQueryRepository favoriteQueryRepository;
	private final SubgroupFavoriteRestaurantRepository subgroupFavoriteRestaurantRepository;
	private final SubgroupFavoriteRestaurantQueryRepository subgroupFavoriteRestaurantQueryRepository;
	private final SubgroupMemberRepository subgroupMemberRepository;
	private final SubgroupRepository subgroupRepository;
	private final RestaurantRepository restaurantRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final FileService fileService;
	private final CursorCodec cursorCodec;
	private final FavoriteAssembler favoriteAssembler;

	@Transactional(readOnly = true)
	public CursorPageResponse<FavoriteRestaurantItem> getMyFavoriteRestaurants(Long memberId, String cursorStr) {
		requireAuthenticated(memberId);

		CursorPageBuilder<FavoriteCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursorStr,
			FavoriteCursor.class);
		if (pageBuilder.isInvalid()) {
			return CursorPageResponse.empty();
		}

		List<FavoriteRestaurantQueryDto> result = favoriteQueryRepository.findFavoriteRestaurants(
			memberId,
			pageBuilder.cursor(),
			CursorPageBuilder.fetchSize(DEFAULT_PAGE_SIZE));

		CursorPageBuilder.Page<FavoriteRestaurantQueryDto> page = pageBuilder.build(
			result,
			DEFAULT_PAGE_SIZE,
			last -> new FavoriteCursor(last.createdAt(), last.favoriteId()));

		List<Long> restaurantIds = page.items().stream()
			.map(FavoriteRestaurantQueryDto::restaurantId)
			.toList();

		List<FavoriteRestaurantItem> items = favoriteAssembler.toFavoriteRestaurantItems(
			page.items(),
			findThumbnails(restaurantIds),
			findCategories(restaurantIds),
			findAddresses(restaurantIds));

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(page.nextCursor(), page.hasNext(), page.size()));
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupFavoriteRestaurantItem> getSubgroupFavoriteRestaurants(
		Long memberId,
		Long subgroupId,
		String cursorStr) {
		requireAuthenticated(memberId);
		requireSubgroupViewPermission(memberId, subgroupId);

		CursorPageBuilder<SubgroupFavoriteCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursorStr,
			SubgroupFavoriteCursor.class);
		if (pageBuilder.isInvalid()) {
			return CursorPageResponse.empty();
		}

		List<SubgroupFavoriteRestaurantQueryDto> result = subgroupFavoriteRestaurantQueryRepository
			.findFavoriteRestaurants(
				subgroupId,
				pageBuilder.cursor(),
				CursorPageBuilder.fetchSize(DEFAULT_PAGE_SIZE));

		CursorPageBuilder.Page<SubgroupFavoriteRestaurantQueryDto> page = pageBuilder.build(
			result,
			DEFAULT_PAGE_SIZE,
			last -> new SubgroupFavoriteCursor(last.createdAt(), last.subgroupFavoriteId()));

		List<Long> restaurantIds = page.items().stream()
			.map(SubgroupFavoriteRestaurantQueryDto::restaurantId)
			.toList();

		List<SubgroupFavoriteRestaurantItem> items = favoriteAssembler.toSubgroupFavoriteRestaurantItems(
			subgroupId,
			page.items(),
			findThumbnails(restaurantIds),
			findCategories(restaurantIds),
			findAddresses(restaurantIds));

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(page.nextCursor(), page.hasNext(), page.size()));
	}

	@Transactional(readOnly = true)
	public FavoritePageTargetsResponse getFavoriteTargets(Long memberId) {
		requireAuthenticated(memberId);
		return createFavoritePageTargetsResponse(memberId);
	}

	@Transactional(readOnly = true)
	public RestaurantFavoriteTargetsResponse getFavoriteTargets(Long memberId, Long restaurantId) {
		requireAuthenticated(memberId);
		validateRestaurant(restaurantId);
		return createRestaurantFavoriteTargetsResponse(memberId, restaurantId);
	}

	@Transactional
	public FavoriteCreateResponse createMyFavorite(Long memberId, Long restaurantId) {
		requireAuthenticated(memberId);
		validateRestaurant(restaurantId);

		MemberFavoriteRestaurant favorite = memberFavoriteRestaurantRepository.findByMemberIdAndRestaurantId(memberId,
			restaurantId)
			.map(existing -> restoreMemberFavoriteIfDeleted(existing))
			.orElseGet(() -> memberFavoriteRestaurantRepository.save(MemberFavoriteRestaurant.create(memberId,
				restaurantId)));

		return favoriteAssembler.toCreateResponse(favorite);
	}

	@Transactional
	public FavoriteCreateResponse createSubgroupFavorite(Long memberId, Long subgroupId, Long restaurantId) {
		requireAuthenticated(memberId);
		validateRestaurant(restaurantId);
		requireSubgroupMembership(memberId, subgroupId);

		SubgroupFavoriteRestaurant favorite = subgroupFavoriteRestaurantRepository
			.findByMemberIdAndSubgroupIdAndRestaurantId(memberId, subgroupId, restaurantId)
			.map(existing -> restoreSubgroupFavoriteIfDeleted(existing))
			.orElseGet(() -> {
				try {
					return subgroupFavoriteRestaurantRepository.save(
						SubgroupFavoriteRestaurant.create(memberId, subgroupId, restaurantId));
				} catch (org.springframework.dao.DataIntegrityViolationException ex) {
					throw new BusinessException(FavoriteErrorCode.FAVORITE_ALREADY_EXISTS);
				}
			});

		return favoriteAssembler.toCreateResponse(favorite);
	}

	@Transactional
	public void deleteMyFavorite(Long memberId, Long restaurantId) {
		requireAuthenticated(memberId);
		validateRestaurant(restaurantId);

		memberFavoriteRestaurantRepository.findByMemberIdAndRestaurantIdAndDeletedAtIsNull(memberId, restaurantId)
			.ifPresent(MemberFavoriteRestaurant::delete);
	}

	@Transactional
	public void deleteSubgroupFavorite(Long memberId, Long subgroupId, Long restaurantId) {
		requireAuthenticated(memberId);
		validateRestaurant(restaurantId);
		requireSubgroupMembership(memberId, subgroupId);

		SubgroupFavoriteRestaurant favorite = subgroupFavoriteRestaurantRepository
			.findBySubgroupIdAndRestaurantId(subgroupId, restaurantId)
			.orElse(null);

		if (favorite == null || favorite.getDeletedAt() != null) {
			return;
		}
		if (!favorite.getMemberId().equals(memberId)) {
			throw new BusinessException(CommonErrorCode.NO_PERMISSION);
		}
		favorite.delete();
	}

	private FavoritePageTargetsResponse createFavoritePageTargetsResponse(Long memberId) {
		long myFavoriteCount = memberFavoriteRestaurantRepository.countByMemberIdAndDeletedAtIsNull(memberId);
		List<FavoriteSubgroupTargetRow> subgroupTargets = subgroupMemberRepository.findFavoriteSubgroupTargets(
			memberId,
			SubgroupStatus.ACTIVE,
			GroupStatus.ACTIVE);
		List<Long> subgroupIds = subgroupTargets.stream().map(FavoriteSubgroupTargetRow::subgroupId).toList();

		Map<Long, Long> subgroupCounts = subgroupIds.isEmpty()
			? Map.of()
			: subgroupFavoriteRestaurantRepository.countBySubgroupIds(subgroupIds)
				.stream()
				.collect(Collectors.toMap(FavoriteCountBySubgroupDto::subgroupId,
					FavoriteCountBySubgroupDto::favoriteCount));

		return favoriteAssembler.toFavoritePageTargetsResponse(
			myFavoriteCount,
			subgroupTargets,
			subgroupCounts);
	}

	private RestaurantFavoriteTargetsResponse createRestaurantFavoriteTargetsResponse(Long memberId,
		Long restaurantId) {
		FavoriteState myFavoriteState = memberFavoriteRestaurantRepository
			.findByMemberIdAndRestaurantIdAndDeletedAtIsNull(memberId, restaurantId)
			.isPresent() ? FavoriteState.FAVORITED : FavoriteState.NOT_FAVORITED;

		List<FavoriteSubgroupTargetRow> subgroupTargets = subgroupMemberRepository.findFavoriteSubgroupTargets(
			memberId,
			SubgroupStatus.ACTIVE,
			GroupStatus.ACTIVE);
		List<Long> subgroupIds = subgroupTargets.stream().map(FavoriteSubgroupTargetRow::subgroupId).toList();

		Set<Long> favoritedSubgroupIds = subgroupIds.isEmpty() ? Set.of()
			: Set.copyOf(subgroupFavoriteRestaurantRepository.findFavoritedSubgroupIds(memberId, subgroupIds,
				restaurantId));

		return favoriteAssembler.toRestaurantFavoriteTargetsResponse(
			myFavoriteState,
			subgroupTargets,
			favoritedSubgroupIds);
	}

	private MemberFavoriteRestaurant restoreMemberFavoriteIfDeleted(MemberFavoriteRestaurant existing) {
		if (existing.getDeletedAt() == null) {
			throw new BusinessException(FavoriteErrorCode.FAVORITE_ALREADY_EXISTS);
		}
		existing.restore();
		return existing;
	}

	private SubgroupFavoriteRestaurant restoreSubgroupFavoriteIfDeleted(SubgroupFavoriteRestaurant existing) {
		if (existing.getDeletedAt() == null) {
			throw new BusinessException(FavoriteErrorCode.FAVORITE_ALREADY_EXISTS);
		}
		existing.restore();
		return existing;
	}

	private Map<Long, String> findThumbnails(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<DomainImageItem>> domainImages = fileService.getDomainImageUrls(
			DomainType.RESTAURANT,
			restaurantIds);
		return domainImages.entrySet().stream()
			.filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
			.collect(java.util.stream.Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getFirst().url()));
	}

	private Map<Long, List<String>> findCategories(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		List<RestaurantCategoryProjection> categories = restaurantFoodCategoryRepository
			.findCategoriesByRestaurantIds(restaurantIds);
		return categories.stream()
			.collect(Collectors.groupingBy(
				RestaurantCategoryProjection::getRestaurantId,
				Collectors.mapping(
					RestaurantCategoryProjection::getCategoryName,
					Collectors.toList())));
	}

	private Map<Long, String> findAddresses(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		return restaurantRepository.findAllById(restaurantIds).stream()
			.filter(r -> r.getDeletedAt() == null)
			.collect(Collectors.toMap(
				Restaurant::getId,
				Restaurant::getFullAddress));
	}

	private void requireAuthenticated(Long memberId) {
		if (memberId == null) {
			throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED);
		}
	}

	private void validateRestaurant(Long restaurantId) {
		if (!restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)) {
			throw new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND);
		}
	}

	private void requireSubgroupMembership(Long memberId, Long subgroupId) {
		var subgroup = subgroupRepository.findByIdAndDeletedAtIsNull(subgroupId)
			.orElseThrow(() -> new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND));
		if (subgroup.getStatus() != SubgroupStatus.ACTIVE) {
			throw new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND);
		}

		subgroupMemberRepository.findBySubgroupIdAndMember_IdAndDeletedAtIsNull(subgroupId, memberId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NO_PERMISSION));
	}

	private void requireSubgroupViewPermission(Long memberId, Long subgroupId) {
		var subgroup = subgroupRepository.findByIdAndDeletedAtIsNull(subgroupId)
			.orElseThrow(() -> new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND));
		if (subgroup.getStatus() != SubgroupStatus.ACTIVE) {
			throw new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND);
		}
		if (subgroup.getJoinType() == com.tasteam.domain.subgroup.type.SubgroupJoinType.OPEN) {
			return;
		}
		subgroupMemberRepository.findBySubgroupIdAndMember_IdAndDeletedAtIsNull(subgroupId, memberId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NO_PERMISSION));
	}
}
