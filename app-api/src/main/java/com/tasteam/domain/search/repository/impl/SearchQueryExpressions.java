package com.tasteam.domain.search.repository.impl;

import java.util.ArrayList;
import java.util.List;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.tasteam.domain.restaurant.entity.QFoodCategory;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.QRestaurantFoodCategory;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.repository.executor.SearchScoreCalculator;

/**
 * QueryDSL 검색 쿼리에서 공통으로 사용하는 표현식 유틸리티.
 * ONE_STEP, TWO_STEP, JOIN_AGGREGATE 전략에서 정적 메서드로 참조한다.
 */
public final class SearchQueryExpressions {

	public static final double MIN_NAME_SIMILARITY = 0.3;

	private SearchQueryExpressions() {}

	public static NumberExpression<Integer> nameExactScore(QRestaurant r, String keywordLower) {
		return new CaseBuilder()
			.when(r.name.lower().eq(keywordLower))
			.then(1)
			.otherwise(0);
	}

	public static NumberExpression<Double> nameSimilarity(QRestaurant r, String keywordLower) {
		return Expressions.numberTemplate(Double.class,
			"cast(function('similarity', lower({0}), lower({1})) as double)", r.name, keywordLower);
	}

	public static BooleanExpression nameSimilar(QRestaurant r, String keywordLower) {
		return nameSimilarity(r, keywordLower).goe(MIN_NAME_SIMILARITY);
	}

	public static NumberExpression<Double> distanceMeters(QRestaurant r, Double latitude, Double longitude) {
		if (latitude == null || longitude == null) {
			return Expressions.numberTemplate(Double.class, "NULL");
		}
		return Expressions.numberTemplate(Double.class,
			"ST_DistanceSphere({0}, ST_MakePoint({1}, {2}))", r.location, longitude, latitude);
	}

	public static NumberExpression<Integer> addressMatchScore(QRestaurant r, String keywordLower) {
		return new CaseBuilder()
			.when(r.fullAddress.lower().contains(keywordLower))
			.then(1)
			.otherwise(0);
	}

	public static NumberExpression<Integer> categoryMatchScore(BooleanExpression categoryExists) {
		return new CaseBuilder()
			.when(categoryExists)
			.then(1)
			.otherwise(0);
	}

	public static NumberExpression<Integer> categoryMatchAggregate(QFoodCategory fc, String keywordLower) {
		return Expressions.numberTemplate(Integer.class,
			"max(case when lower({0}) = {1} then 1 else 0 end)", fc.name, keywordLower);
	}

	public static BooleanExpression categoryMatchExists(String keywordLower, QRestaurant r) {
		QRestaurantFoodCategory rfc = QRestaurantFoodCategory.restaurantFoodCategory;
		QFoodCategory fc = QFoodCategory.foodCategory;

		return JPAExpressions.selectOne()
			.from(rfc)
			.join(rfc.foodCategory, fc)
			.where(
				rfc.restaurant.eq(r),
				fc.name.lower().eq(keywordLower))
			.exists();
	}

	public static NumberExpression<Double> totalScore(NumberExpression<Integer> nameExactScore,
		NumberExpression<Double> nameSimilarity, NumberExpression<Double> distanceMeters, Double radiusMeters) {
		NumberExpression<Double> distanceWeight = distanceWeight(distanceMeters, radiusMeters);
		return nameExactScore.doubleValue()
			.multiply(100.0)
			.add(nameSimilarity.multiply(30.0))
			.add(distanceWeight.multiply(50.0));
	}

	public static NumberExpression<Double> distanceWeight(NumberExpression<Double> distanceMeters,
		Double radiusMeters) {
		if (radiusMeters == null) {
			return Expressions.numberTemplate(Double.class, "0.0");
		}
		return Expressions.numberTemplate(Double.class,
			"cast(greatest(0.0, 1.0 - (cast({0} as double) / cast({1} as double))) as double)",
			distanceMeters, radiusMeters);
	}

	public static BooleanExpression distanceFilter(QRestaurant r, Double latitude, Double longitude,
		Double radiusMeters) {
		if (radiusMeters == null || latitude == null || longitude == null) {
			return null;
		}
		return Expressions.numberTemplate(Integer.class,
			"function('st_dwithin_geo', {0}, {1}, {2}, {3})",
			r.location, longitude, latitude, radiusMeters).eq(1);
	}

	public static BooleanExpression cursorCondition(SearchCursor cursor, QRestaurant r,
		NumberExpression<Double> totalScore, Double radiusMeters) {
		if (cursor == null) {
			return null;
		}

		double cursorScore = cursorScore(cursor, radiusMeters);
		BooleanExpression cond = null;
		cond = or(cond, totalScore.lt(cursorScore));
		cond = or(cond, totalScore.eq(cursorScore).and(r.updatedAt.lt(cursor.updatedAt())));
		cond = or(cond, totalScore.eq(cursorScore)
			.and(r.updatedAt.eq(cursor.updatedAt()))
			.and(r.id.lt(cursor.id())));

		return cond;
	}

	public static List<OrderSpecifier<?>> scoreOrderSpecifiers(QRestaurant r,
		NumberExpression<Double> totalScore) {
		List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
		orderSpecifiers.add(totalScore.desc());
		orderSpecifiers.add(r.updatedAt.desc());
		orderSpecifiers.add(r.id.desc());
		return orderSpecifiers;
	}

	private static double cursorScore(SearchCursor cursor, Double radiusMeters) {
		return SearchScoreCalculator.cursorScore(cursor, radiusMeters);
	}

	private static BooleanExpression or(BooleanExpression base, BooleanExpression next) {
		return base == null ? next : base.or(next);
	}
}
