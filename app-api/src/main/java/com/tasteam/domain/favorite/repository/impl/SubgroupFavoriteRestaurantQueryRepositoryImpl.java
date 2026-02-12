package com.tasteam.domain.favorite.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.favorite.dto.SubgroupFavoriteCursor;
import com.tasteam.domain.favorite.dto.SubgroupFavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.entity.QSubgroupFavoriteRestaurant;
import com.tasteam.domain.favorite.entity.SubgroupFavoriteRestaurant;
import com.tasteam.domain.favorite.repository.SubgroupFavoriteRestaurantQueryRepository;
import com.tasteam.domain.restaurant.entity.QRestaurant;

@Repository
public class SubgroupFavoriteRestaurantQueryRepositoryImpl extends QueryDslSupport
	implements SubgroupFavoriteRestaurantQueryRepository {

	public SubgroupFavoriteRestaurantQueryRepositoryImpl() {
		super(SubgroupFavoriteRestaurant.class);
	}

	@Override
	public List<SubgroupFavoriteRestaurantQueryDto> findFavoriteRestaurants(Long subgroupId,
		SubgroupFavoriteCursor cursor,
		int size) {
		QSubgroupFavoriteRestaurant sf = QSubgroupFavoriteRestaurant.subgroupFavoriteRestaurant;
		QRestaurant r = QRestaurant.restaurant;

		return getQueryFactory()
			.select(Projections.constructor(
				SubgroupFavoriteRestaurantQueryDto.class,
				sf.id,
				r.id,
				r.name,
				sf.createdAt))
			.from(sf)
			.join(r).on(r.id.eq(sf.restaurantId).and(r.deletedAt.isNull()))
			.where(
				sf.subgroupId.eq(subgroupId),
				sf.deletedAt.isNull(),
				cursorCondition(cursor, sf))
			.orderBy(sf.createdAt.desc(), sf.id.desc())
			.limit(size)
			.fetch();
	}

	private BooleanExpression cursorCondition(SubgroupFavoriteCursor cursor, QSubgroupFavoriteRestaurant sf) {
		if (cursor == null) {
			return null;
		}
		return sf.createdAt.lt(cursor.createdAt())
			.or(sf.createdAt.eq(cursor.createdAt()).and(sf.id.lt(cursor.id())));
	}
}
