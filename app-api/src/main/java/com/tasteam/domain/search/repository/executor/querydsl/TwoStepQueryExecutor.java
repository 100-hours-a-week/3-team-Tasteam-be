package com.tasteam.domain.search.repository.executor.querydsl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.RestaurantSearchRow;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;
import com.tasteam.domain.search.repository.impl.SearchQueryExpressions;

/**
 * TWO_STEP 전략 실행기.
 * 1단계: candidateLimit 개수의 ID를 커서·필터·정렬 조건으로 수집한다.
 * 2단계: 수집한 ID에 대해 전체 스코어 컬럼을 SELECT해 최종 페이지를 반환한다.
 * 1단계 쿼리가 인덱스를 최대한 활용할 수 있도록 SELECT 컬럼을 id만으로 최소화하는 것이 핵심이다.
 */
@Component
public class TwoStepQueryExecutor extends QueryDslSupport implements SearchQueryExecutor {

	private final SearchQueryProperties properties;

	public TwoStepQueryExecutor(SearchQueryProperties properties) {
		super(Restaurant.class);
		this.properties = properties;
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.TWO_STEP;
	}

	@Override
	public List<SearchRestaurantCursorRow> execute(String keyword, SearchCursor cursor, int size,
		Double latitude, Double longitude, Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = SearchQueryExpressions.nameSimilar(r, kw);
		NumberExpression<Integer> nameExactScore = SearchQueryExpressions.nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = SearchQueryExpressions.nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = SearchQueryExpressions.distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = SearchQueryExpressions.categoryMatchScore(
			SearchQueryExpressions.categoryMatchExists(kw, r));
		NumberExpression<Integer> addressScore = SearchQueryExpressions.addressMatchScore(r, kw);
		NumberExpression<Double> totalScore = SearchQueryExpressions.totalScore(nameExactScore, nameSimilarity,
			distanceExpr, radiusMeters);

		List<Long> candidateIds = getQueryFactory()
			.select(r.id)
			.from(r)
			.where(
				r.deletedAt.isNull(),
				nameContains.or(nameSimilar),
				SearchQueryExpressions.distanceFilter(r, latitude, longitude, radiusMeters),
				SearchQueryExpressions.cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(SearchQueryExpressions.scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(properties.getCandidateLimit())
			.fetch();

		if (candidateIds.isEmpty()) {
			return List.of();
		}

		return getQueryFactory()
			.select(Projections.constructor(
				SearchRestaurantCursorRow.class,
				Projections.constructor(RestaurantSearchRow.class, r.id, r.name, r.fullAddress),
				nameExactScore,
				nameSimilarity,
				Expressions.nullExpression(Double.class),
				distanceExpr,
				categoryScore,
				addressScore,
				r.updatedAt))
			.from(r)
			.where(
				r.id.in(candidateIds),
				SearchQueryExpressions.cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(SearchQueryExpressions.scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}
}
