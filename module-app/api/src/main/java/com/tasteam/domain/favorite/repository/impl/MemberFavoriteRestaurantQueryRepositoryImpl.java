package com.tasteam.domain.favorite.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.favorite.dto.FavoriteCursor;
import com.tasteam.domain.favorite.dto.FavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.entity.MemberFavoriteRestaurant;
import com.tasteam.domain.favorite.entity.QMemberFavoriteRestaurant;
import com.tasteam.domain.favorite.repository.MemberFavoriteRestaurantQueryRepository;
import com.tasteam.domain.restaurant.entity.QRestaurant;

@Repository
public class MemberFavoriteRestaurantQueryRepositoryImpl extends QueryDslSupport
	implements MemberFavoriteRestaurantQueryRepository {

	public MemberFavoriteRestaurantQueryRepositoryImpl() {
		super(MemberFavoriteRestaurant.class);
	}

	@Override
	public List<FavoriteRestaurantQueryDto> findFavoriteRestaurants(Long memberId, FavoriteCursor cursor, int size) {
		QMemberFavoriteRestaurant f = QMemberFavoriteRestaurant.memberFavoriteRestaurant;
		QRestaurant r = QRestaurant.restaurant;

		return getQueryFactory()
			.select(Projections.constructor(
				FavoriteRestaurantQueryDto.class,
				f.id,
				r.id,
				r.name,
				f.createdAt))
			.from(f)
			.join(r).on(r.id.eq(f.restaurantId).and(r.deletedAt.isNull()))
			.where(
				f.memberId.eq(memberId),
				f.deletedAt.isNull(),
				cursorCondition(cursor, f))
			.orderBy(f.createdAt.desc(), f.id.desc())
			.limit(size)
			.fetch();
	}

	private BooleanExpression cursorCondition(FavoriteCursor cursor, QMemberFavoriteRestaurant f) {
		if (cursor == null) {
			return null;
		}
		return f.createdAt.lt(cursor.createdAt())
			.or(f.createdAt.eq(cursor.createdAt()).and(f.id.lt(cursor.id())));
	}
}
