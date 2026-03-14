package com.tasteam.domain.search.repository.executor.querydsl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;
import com.tasteam.domain.search.repository.impl.SearchQueryExpressions;

/**
 * [ONE_STEP] 단일 QueryDSL 쿼리로 필터링·스코어링·정렬을 한 번에 수행하는 전략.
 *
 * <p>흐름: WHERE(이름 LIKE/유사도 + 거리 필터) → SELECT(스코어 계산) → ORDER BY 스코어 DESC → LIMIT
 *
 * <p>장점: 단순하고 구현이 명확하다. 단점: 후보군이 많을 때 DB가 모든 행의 스코어를 계산해야 한다.
 */
@Component
public class OneStepQueryExecutor extends QueryDslSupport implements SearchQueryExecutor {

	public OneStepQueryExecutor() {
		super(Restaurant.class);
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.ONE_STEP;
	}

	@Override
	public List<SearchRestaurantCursorRow> execute(String keyword, SearchCursor cursor, int size, Double latitude,
		Double longitude, Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = SearchQueryExpressions.nameSimilar(r, kw);
		BooleanExpression categoryExists = SearchQueryExpressions.categoryMatchExists(kw, r);
		NumberExpression<Integer> nameExactScore = SearchQueryExpressions.nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = SearchQueryExpressions.nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = SearchQueryExpressions.distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = SearchQueryExpressions.categoryMatchScore(categoryExists);
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
			.where(
				r.deletedAt.isNull(),
				nameContains.or(nameSimilar),
				SearchQueryExpressions.distanceFilter(r, latitude, longitude, radiusMeters),
				SearchQueryExpressions.cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(SearchQueryExpressions.scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}
}
