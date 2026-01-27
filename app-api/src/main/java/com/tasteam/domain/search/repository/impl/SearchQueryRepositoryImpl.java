package com.tasteam.domain.search.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.repository.SearchQueryRepository;

@Repository
public class SearchQueryRepositoryImpl extends QueryDslSupport implements SearchQueryRepository {

	public SearchQueryRepositoryImpl() {
		super(Restaurant.class);
	}

	@Override
	public List<Restaurant> searchRestaurantsByKeyword(String keyword, SearchCursor cursor, int size) {
		QRestaurant r = QRestaurant.restaurant;

		return getQueryFactory()
			.selectFrom(r)
			.where(
				r.deletedAt.isNull(),
				keywordCondition(keyword, r),
				cursorCondition(cursor, r))
			.orderBy(r.updatedAt.desc(), r.id.desc())
			.limit(size)
			.fetch();
	}

	private BooleanExpression keywordCondition(String keyword, QRestaurant r) {
		String pattern = "%" + keyword.toLowerCase() + "%";
		return r.name.lower().like(pattern)
			.or(r.fullAddress.lower().like(pattern));
	}

	private BooleanExpression cursorCondition(SearchCursor cursor, QRestaurant r) {
		if (cursor == null) {
			return null;
		}
		return r.updatedAt.lt(cursor.updatedAt())
			.or(r.updatedAt.eq(cursor.updatedAt()).and(r.id.lt(cursor.id())));
	}
}
