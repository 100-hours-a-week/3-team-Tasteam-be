package com.tasteam.domain.search.repository.executor.querydsl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QFoodCategory;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.QRestaurantFoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.RestaurantSearchRow;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;
import com.tasteam.domain.search.repository.impl.SearchQueryExpressions;

/**
 * JOIN_AGGREGATE 전략 실행기.
 * restaurant ↔ restaurant_food_category ↔ food_category를 LEFT JOIN 후
 * GROUP BY r.id로 카테고리 매치를 집계(MAX CASE)한다.
 * EXISTS 서브쿼리 대신 JOIN을 사용하므로 카테고리가 있는 레스토랑에 인덱스 친화적인 접근이 가능하나,
 * GROUP BY 비용이 발생한다.
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
	public List<SearchRestaurantCursorRow> execute(String keyword, SearchCursor cursor, int size,
		Double latitude, Double longitude, Double radiusMeters) {
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
				Projections.constructor(RestaurantSearchRow.class, r.id, r.name, r.fullAddress, r.updatedAt),
				nameExactScore,
				nameSimilarity,
				Expressions.nullExpression(Double.class),
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
