package com.tasteam.domain.favorite.dto.response;

import java.util.List;

public record FavoriteTargetsResponse(
	List<FavoriteTargetItem> targets) {
}
