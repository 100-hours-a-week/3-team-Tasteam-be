package com.tasteam.domain.search.repository.executor.querydsl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QFoodCategory;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.QRestaurantFoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;
import com.tasteam.domain.search.repository.impl.SearchQueryExpressions;

/**
 * [JOIN_AGGREGATE] restaurant_food_category를 LEFT JOIN 후 GROUP BY로 카테고리 매칭 점수를 집계하는 전략.
 *
 * <p>흐름: LEFT JOIN food_category → GROUP BY r.id → SELECT(카테고리 MAX 집계로 스코어 계산) → ORDER BY 스코어 DESC
 * → LIMIT
 *
 * <p>장점: 서브쿼리 없이 JOIN으로 카테고리 점수를 정확히 반영한다. 단점: GROUP BY로 인해 집계 비용이 발생하며, 카테고리가 많은 식당일수록
 * 중간 행이 늘어난다.
 */
@Component
public class JoinAggregateQueryExecutor extends QueryDslSupport implements SearchQueryExecutor {

	public JoinAggregateQueryExecutor() {
		super(Restaurant.class);
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.JOIN_AGGREGATE;
	}

	@Override
	public List<SearchRestaurantCursorRow> execute(String keyword, SearchCursor cursor, int size, Double latitude,
		Double longitude, Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		QRestaurantFoodCategory rfc = QRestaurantFoodCategory.restaurantFoodCategory;
		QFoodCategory fc = QFoodCategory.foodCategory;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = SearchQueryExpressions.nameSimilar(r, kw);
		NumberExpression<Integer> nameExactScore = SearchQueryExpressions.nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = SearchQueryExpressions.nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = SearchQueryExpressions.distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = SearchQueryExpressions.categoryMatchAggregate(fc, kw);
		NumberExpression<Integer> addressScore = SearchQueryExpressions.addressMatchScore(r, kw);
		NumberExpression<Double> totalScore = SearchQueryExpressions.totalScore(nameExactScore, nameSimilarity,
			distanceExpr, radiusMeters);

		return getQueryFactory()
			.select(Projections.constructor(
				SearchRestaurantCursorRow.class,
				r,
				nameExactScore,
				nameSimilarity,
				distanceExpr,
				categoryScore,
				addressScore))
			.from(r)
			.leftJoin(rfc).on(rfc.restaurant.eq(r))
			.leftJoin(rfc.foodCategory, fc)
			.where(
				r.deletedAt.isNull(),
				nameContains.or(nameSimilar),
				SearchQueryExpressions.distanceFilter(r, latitude, longitude, radiusMeters),
				SearchQueryExpressions.cursorCondition(cursor, r, totalScore, radiusMeters))
			.groupBy(r.id)
			.orderBy(SearchQueryExpressions.scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}
}
