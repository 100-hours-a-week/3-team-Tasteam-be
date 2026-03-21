package com.tasteam.domain.favorite.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.favorite.dto.SubgroupFavoriteCursor;
import com.tasteam.domain.favorite.dto.SubgroupFavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.entity.QSubgroupFavoriteRestaurant;
import com.tasteam.domain.favorite.entity.SubgroupFavoriteRestaurant;
import com.tasteam.domain.favorite.repository.SubgroupFavoriteRestaurantQueryRepository;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.subgroup.entity.QSubgroup;

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
		QSubgroupFavoriteRestaurant sfCount = new QSubgroupFavoriteRestaurant("sfCount");
		QRestaurant r = QRestaurant.restaurant;
		QSubgroup subgroup = QSubgroup.subgroup;
		DateTimeExpression<java.time.Instant> createdAtMax = sf.createdAt.max();
		NumberExpression<Long> idMax = sf.id.max();
		var subgroupFavoriteCount = JPAExpressions
			.select(sfCount.memberId.countDistinct())
			.from(sfCount)
			.where(
				sfCount.deletedAt.isNull(),
				sfCount.restaurantId.eq(r.id),
				sfCount.subgroupId.eq(subgroupId));

		return getQueryFactory()
			.select(Projections.constructor(
				SubgroupFavoriteRestaurantQueryDto.class,
				idMax,
				r.id,
				r.name,
				createdAtMax,
				subgroupFavoriteCount))
			.from(sf)
			.join(subgroup).on(subgroup.id.eq(sf.subgroupId))
			.join(r).on(r.id.eq(sf.restaurantId).and(r.deletedAt.isNull()))
			.where(
				subgroup.id.eq(subgroupId),
				subgroup.deletedAt.isNull(),
				sf.deletedAt.isNull())
			.groupBy(r.id, r.name)
			.having(cursorCondition(cursor, createdAtMax, idMax))
			.orderBy(createdAtMax.desc(), idMax.desc())
			.limit(size)
			.fetch();
	}

	private BooleanExpression cursorCondition(
		SubgroupFavoriteCursor cursor,
		DateTimeExpression<java.time.Instant> createdAtMax,
		NumberExpression<Long> idMax) {
		if (cursor == null) {
			return null;
		}
		return createdAtMax.lt(cursor.createdAt())
			.or(createdAtMax.eq(cursor.createdAt()).and(idMax.lt(cursor.id())));
	}
}
