package com.tasteam.domain.search.repository.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QFoodCategory;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.QRestaurantFoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryRepository;
import com.tasteam.domain.search.repository.SearchQueryStrategy;

@Repository
public class SearchQueryRepositoryImpl extends QueryDslSupport implements SearchQueryRepository {

	private final SearchQueryProperties properties;
	private static final double MIN_NAME_SIMILARITY = 0.3;

	public SearchQueryRepositoryImpl(SearchQueryProperties properties) {
		super(Restaurant.class);
		this.properties = properties;
	}

	@Override
	public List<SearchRestaurantCursorRow> searchRestaurantsByKeyword(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		if (properties.getStrategy() == SearchQueryStrategy.TWO_STEP) {
			return searchTwoStep(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		if (properties.getStrategy() == SearchQueryStrategy.JOIN_AGGREGATE) {
			return searchJoinAggregate(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		return searchOneStep(keyword, cursor, size, latitude, longitude, radiusMeters);
	}

	private List<SearchRestaurantCursorRow> searchOneStep(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = nameSimilar(r, kw);
		BooleanExpression categoryExists = categoryMatchExists(kw, r);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchScore(categoryExists);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);
		NumberExpression<Double> totalScore = totalScore(nameExactScore, nameSimilarity, distanceExpr, radiusMeters);

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
				distanceFilter(distanceExpr, radiusMeters),
				cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<SearchRestaurantCursorRow> searchTwoStep(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = nameSimilar(r, kw);
		BooleanExpression categoryExists = categoryMatchExists(kw, r);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchScore(categoryExists);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);
		NumberExpression<Double> totalScore = totalScore(nameExactScore, nameSimilarity, distanceExpr, radiusMeters);

		List<Long> candidateIds = getQueryFactory()
			.select(r.id)
			.from(r)
			.where(
				r.deletedAt.isNull(),
				nameContains.or(nameSimilar),
				distanceFilter(distanceExpr, radiusMeters),
				cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
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
				cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<SearchRestaurantCursorRow> searchJoinAggregate(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		QRestaurantFoodCategory rfc = QRestaurantFoodCategory.restaurantFoodCategory;
		QFoodCategory fc = QFoodCategory.foodCategory;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = nameSimilar(r, kw);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchAggregate(fc, kw);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);
		NumberExpression<Double> totalScore = totalScore(nameExactScore, nameSimilarity, distanceExpr, radiusMeters);

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
				distanceFilter(distanceExpr, radiusMeters),
				cursorCondition(cursor, r, totalScore, radiusMeters))
			.groupBy(r.id)
			.orderBy(scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<OrderSpecifier<?>> scoreOrderSpecifiers(QRestaurant r, NumberExpression<Double> totalScore) {
		List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
		orderSpecifiers.add(totalScore.desc());
		orderSpecifiers.add(r.updatedAt.desc());
		orderSpecifiers.add(r.id.desc());
		return orderSpecifiers;
	}

	private NumberExpression<Integer> nameExactScore(QRestaurant r, String keywordLower) {
		return new CaseBuilder()
			.when(r.name.lower().eq(keywordLower))
			.then(1)
			.otherwise(0);
	}

	private NumberExpression<Double> nameSimilarity(QRestaurant r, String keywordLower) {
		return Expressions.numberTemplate(Double.class, "similarity(lower({0}), lower({1}))", r.name, keywordLower);
	}

	private BooleanExpression nameSimilar(QRestaurant r, String keywordLower) {
		return nameSimilarity(r, keywordLower).goe(MIN_NAME_SIMILARITY);
	}

	private NumberExpression<Double> distanceMeters(QRestaurant r, Double latitude, Double longitude) {
		if (latitude == null || longitude == null) {
			return Expressions.numberTemplate(Double.class, "NULL");
		}
		return Expressions.numberTemplate(Double.class,
			"ST_DistanceSphere({0}, ST_MakePoint({1}, {2}))", r.location, longitude, latitude);
	}

	private NumberExpression<Integer> addressMatchScore(QRestaurant r, String keywordLower) {
		return new CaseBuilder()
			.when(r.fullAddress.lower().contains(keywordLower))
			.then(1)
			.otherwise(0);
	}

	private NumberExpression<Integer> categoryMatchScore(BooleanExpression categoryExists) {
		return new CaseBuilder()
			.when(categoryExists)
			.then(1)
			.otherwise(0);
	}

	private NumberExpression<Integer> categoryMatchAggregate(QFoodCategory fc, String keywordLower) {
		return Expressions.numberTemplate(Integer.class,
			"max(case when lower({0}) = {1} then 1 else 0 end)", fc.name, keywordLower);
	}

	private BooleanExpression categoryMatchExists(String keywordLower, QRestaurant r) {
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

	private NumberExpression<Double> totalScore(NumberExpression<Integer> nameExactScore,
		NumberExpression<Double> nameSimilarity, NumberExpression<Double> distanceMeters, Double radiusMeters) {
		NumberExpression<Double> distanceWeight = distanceWeight(distanceMeters, radiusMeters);
		return nameExactScore.doubleValue()
			.multiply(100.0)
			.add(nameSimilarity.multiply(30.0))
			.add(distanceWeight.multiply(50.0));
	}

	private NumberExpression<Double> distanceWeight(NumberExpression<Double> distanceMeters, Double radiusMeters) {
		if (radiusMeters == null) {
			return Expressions.numberTemplate(Double.class, "0");
		}
		return Expressions.numberTemplate(Double.class,
			"GREATEST(0, 1 - ({0} / {1}))", distanceMeters, radiusMeters);
	}

	private BooleanExpression distanceFilter(NumberExpression<Double> distanceMeters, Double radiusMeters) {
		if (radiusMeters == null) {
			return null;
		}
		return distanceMeters.loe(radiusMeters);
	}

	private BooleanExpression cursorCondition(SearchCursor cursor, QRestaurant r,
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

	private double cursorScore(SearchCursor cursor, Double radiusMeters) {
		double nameExact = cursor.nameExact() == null ? 0.0 : cursor.nameExact();
		double similarity = cursor.nameSimilarity() == null ? 0.0 : cursor.nameSimilarity();
		double distanceWeight = 0.0;
		if (cursor.distanceMeters() != null && radiusMeters != null) {
			double effectiveRadius = Math.max(radiusMeters, 1.0);
			distanceWeight = Math.max(0.0, 1.0 - (cursor.distanceMeters() / effectiveRadius));
		}
		return nameExact * 100.0 + similarity * 30.0 + distanceWeight * 50.0;
	}

	private BooleanExpression or(BooleanExpression base, BooleanExpression next) {
		return base == null ? next : base.or(next);
	}
}
