package com.tasteam.domain.favorite.dto.response;

import java.util.List;

public record FavoritePageTargetsResponse(
	MyFavoriteTarget myFavorite,
	List<SubgroupFavoriteTarget> subgroupFavorites) {

	public record MyFavoriteTarget(
		Long favoriteCount) {
	}

	public record SubgroupFavoriteTarget(
		Long subgroupId,
		String name,
		Long favoriteCount) {
	}
}
