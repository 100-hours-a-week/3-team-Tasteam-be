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
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;
import com.tasteam.domain.search.repository.impl.SearchQueryExpressions;

/**
 * [TWO_STEP] 후보 ID만 먼저 추려낸 뒤 실제 엔티티를 두 번째 쿼리로 조회하는 전략.
 *
 * <p>흐름: 1차 — id SELECT + ORDER BY 스코어 DESC LIMIT candidateLimit → 2차 — id IN(…) 로 엔티티 조회 + 재정렬 →
 * LIMIT size
 *
 * <p>장점: 1차에서 가벼운 id 스캔만 하므로 대규모 데이터에서 메모리 효율이 높다. 단점: 쿼리가 두 번 실행되고, candidateLimit 설정이 결과
 * 품질에 영향을 준다.
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
				r,
				nameExactScore,
				nameSimilarity,
				distanceExpr,
				categoryScore,
				addressScore))
			.from(r)
			.where(
				r.id.in(candidateIds),
				SearchQueryExpressions.cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(SearchQueryExpressions.scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}
}
