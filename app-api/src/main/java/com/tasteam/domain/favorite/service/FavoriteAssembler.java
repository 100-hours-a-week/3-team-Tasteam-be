package com.tasteam.domain.favorite.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tasteam.domain.favorite.dto.FavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.dto.FavoriteSubgroupTargetRow;
import com.tasteam.domain.favorite.dto.SubgroupFavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.dto.response.FavoriteCreateResponse;
import com.tasteam.domain.favorite.dto.response.FavoritePageTargetsResponse;
import com.tasteam.domain.favorite.dto.response.FavoriteRestaurantItem;
import com.tasteam.domain.favorite.dto.response.RestaurantFavoriteTargetItem;
import com.tasteam.domain.favorite.dto.response.RestaurantFavoriteTargetsResponse;
import com.tasteam.domain.favorite.dto.response.SubgroupFavoriteRestaurantItem;
import com.tasteam.domain.favorite.entity.MemberFavoriteRestaurant;
import com.tasteam.domain.favorite.entity.SubgroupFavoriteRestaurant;
import com.tasteam.domain.favorite.type.FavoriteState;
import com.tasteam.domain.favorite.type.FavoriteTargetType;

@Component
public class FavoriteAssembler {

	public FavoriteCreateResponse toCreateResponse(MemberFavoriteRestaurant favorite) {
		return new FavoriteCreateResponse(favorite.getId(), favorite.getRestaurantId(), favorite.getCreatedAt());
	}

	public FavoriteCreateResponse toCreateResponse(SubgroupFavoriteRestaurant favorite) {
		return new FavoriteCreateResponse(favorite.getId(), favorite.getRestaurantId(), favorite.getCreatedAt());
	}

	public List<FavoriteRestaurantItem> toFavoriteRestaurantItems(
		List<FavoriteRestaurantQueryDto> items,
		Map<Long, String> thumbnails) {
		return items.stream()
			.map(dto -> new FavoriteRestaurantItem(
				dto.restaurantId(),
				dto.restaurantName(),
				thumbnails.get(dto.restaurantId()),
				dto.createdAt()))
			.toList();
	}

	public List<SubgroupFavoriteRestaurantItem> toSubgroupFavoriteRestaurantItems(
		Long subgroupId,
		List<SubgroupFavoriteRestaurantQueryDto> items,
		Map<Long, String> thumbnails) {
		return items.stream()
			.map(dto -> new SubgroupFavoriteRestaurantItem(
				dto.restaurantId(),
				dto.restaurantName(),
				thumbnails.get(dto.restaurantId()),
				subgroupId,
				dto.createdAt()))
			.toList();
	}

	public RestaurantFavoriteTargetsResponse toRestaurantFavoriteTargetsResponse(
		FavoriteState myFavoriteState,
		List<FavoriteSubgroupTargetRow> subgroupTargets,
		Set<Long> favoritedSubgroupIds) {
		RestaurantFavoriteTargetItem myTarget = new RestaurantFavoriteTargetItem(
			FavoriteTargetType.ME,
			null,
			"내 찜",
			myFavoriteState);

		List<RestaurantFavoriteTargetItem> subgroupItems = subgroupTargets.stream()
			.map(subgroup -> new RestaurantFavoriteTargetItem(
				FavoriteTargetType.SUBGROUP,
				subgroup.subgroupId(),
				subgroup.subgroupName(),
				favoritedSubgroupIds.contains(subgroup.subgroupId()) ? FavoriteState.FAVORITED
					: FavoriteState.NOT_FAVORITED))
			.toList();

		return new RestaurantFavoriteTargetsResponse(
			java.util.stream.Stream.concat(java.util.stream.Stream.of(myTarget), subgroupItems.stream()).toList());
	}

	public FavoritePageTargetsResponse toFavoritePageTargetsResponse(
		long myFavoriteCount,
		List<FavoriteSubgroupTargetRow> subgroupTargets,
		Map<Long, Long> subgroupFavoriteCounts) {
		FavoritePageTargetsResponse.MyFavoriteTarget myFavorite = new FavoritePageTargetsResponse.MyFavoriteTarget(
			myFavoriteCount);

		List<FavoritePageTargetsResponse.SubgroupFavoriteTarget> subgroupFavorites = subgroupTargets.stream()
			.map(subgroup -> new FavoritePageTargetsResponse.SubgroupFavoriteTarget(
				subgroup.subgroupId(),
				subgroup.subgroupName(),
				subgroupFavoriteCounts.getOrDefault(subgroup.subgroupId(), 0L)))
			.toList();

		return new FavoritePageTargetsResponse(myFavorite, subgroupFavorites);
	}
}
