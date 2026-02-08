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

	public SearchQueryRepositoryImpl(SearchQueryProperties properties) {
		super(Restaurant.class);
		this.properties = properties;
	}

	@Override
	public List<SearchRestaurantCursorRow> searchRestaurantsByKeyword(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude) {
		if (properties.getStrategy() == SearchQueryStrategy.TWO_STEP) {
			return searchTwoStep(keyword, cursor, size, latitude, longitude);
		}
		if (properties.getStrategy() == SearchQueryStrategy.JOIN_AGGREGATE) {
			return searchJoinAggregate(keyword, cursor, size, latitude, longitude);
		}
		return searchOneStep(keyword, cursor, size, latitude, longitude);
	}

	private List<SearchRestaurantCursorRow> searchOneStep(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude) {
		QRestaurant r = QRestaurant.restaurant;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression addrContains = r.fullAddress.lower().contains(kw);
		BooleanExpression categoryExists = categoryMatchExists(kw, r);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchScore(categoryExists);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);

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
				nameContains.or(addrContains).or(categoryExists),
				cursorCondition(cursor, r, kw, latitude, longitude, categoryExists))
			.orderBy(buildOrderSpecifiers(r, kw, latitude, longitude, categoryExists).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<SearchRestaurantCursorRow> searchTwoStep(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude) {
		QRestaurant r = QRestaurant.restaurant;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression addrContains = r.fullAddress.lower().contains(kw);
		BooleanExpression categoryExists = categoryMatchExists(kw, r);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchScore(categoryExists);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);

		List<Long> candidateIds = getQueryFactory()
			.select(r.id)
			.from(r)
			.where(
				r.deletedAt.isNull(),
				nameContains.or(addrContains).or(categoryExists),
				cursorCondition(cursor, r, kw, latitude, longitude, categoryExists))
			.orderBy(buildOrderSpecifiers(r, kw, latitude, longitude, categoryExists).toArray(OrderSpecifier[]::new))
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
				cursorCondition(cursor, r, kw, latitude, longitude, categoryExists))
			.orderBy(buildOrderSpecifiers(r, kw, latitude, longitude, categoryExists).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<SearchRestaurantCursorRow> searchJoinAggregate(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude) {
		QRestaurant r = QRestaurant.restaurant;
		QRestaurantFoodCategory rfc = QRestaurantFoodCategory.restaurantFoodCategory;
		QFoodCategory fc = QFoodCategory.foodCategory;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression addrContains = r.fullAddress.lower().contains(kw);
		BooleanExpression categoryNameMatch = fc.name.lower().eq(kw);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchAggregate(fc, kw);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);

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
				nameContains.or(addrContains).or(categoryNameMatch),
				cursorCondition(cursor, r, kw, latitude, longitude, categoryScore))
			.groupBy(r.id)
			.orderBy(buildOrderSpecifiers(r, kw, latitude, longitude, categoryScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<OrderSpecifier<?>> buildOrderSpecifiers(QRestaurant r, String keywordLower, Double latitude,
		Double longitude, BooleanExpression categoryExists) {
		List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
		orderSpecifiers.add(nameExactScore(r, keywordLower).desc());
		orderSpecifiers.add(nameSimilarity(r, keywordLower).desc());
		if (latitude != null && longitude != null) {
			orderSpecifiers.add(distanceMeters(r, latitude, longitude).asc());
		}
		orderSpecifiers.add(categoryMatchScore(categoryExists).desc());
		orderSpecifiers.add(addressMatchScore(r, keywordLower).desc());
		orderSpecifiers.add(r.updatedAt.desc());
		orderSpecifiers.add(r.id.desc());
		return orderSpecifiers;
	}

	private List<OrderSpecifier<?>> buildOrderSpecifiers(QRestaurant r, String keywordLower, Double latitude,
		Double longitude, NumberExpression<Integer> categoryScore) {
		List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
		orderSpecifiers.add(nameExactScore(r, keywordLower).desc());
		orderSpecifiers.add(nameSimilarity(r, keywordLower).desc());
		if (latitude != null && longitude != null) {
			orderSpecifiers.add(distanceMeters(r, latitude, longitude).asc());
		}
		orderSpecifiers.add(categoryScore.desc());
		orderSpecifiers.add(addressMatchScore(r, keywordLower).desc());
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

	private BooleanExpression cursorCondition(SearchCursor cursor, QRestaurant r, String keywordLower,
		Double latitude, Double longitude, BooleanExpression categoryExists) {
		if (cursor == null) {
			return null;
		}

		NumberExpression<Integer> nameExactScore = nameExactScore(r, keywordLower);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, keywordLower);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchScore(categoryExists);
		NumberExpression<Integer> addressScore = addressMatchScore(r, keywordLower);

		BooleanExpression cond = null;
		cond = or(cond, nameExactScore.lt(cursor.nameExact()));
		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.lt(cursor.nameSimilarity())));

		if (latitude != null && longitude != null) {
			NumberExpression<Double> distanceSortExpr = Expressions.numberTemplate(Double.class,
				"COALESCE({0}, {1})", distanceExpr, 1e15);
			double cursorDist = cursor.distanceMeters() == null ? 1e15 : cursor.distanceMeters();
			cond = or(cond, nameExactScore.eq(cursor.nameExact())
				.and(nameSimilarity.eq(cursor.nameSimilarity()))
				.and(distanceSortExpr.gt(cursorDist)));
		}

		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.eq(cursor.nameSimilarity()))
			.and(distanceComparable(latitude, longitude, distanceExpr, cursor))
			.and(categoryScore.lt(cursor.categoryMatch())));

		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.eq(cursor.nameSimilarity()))
			.and(distanceComparable(latitude, longitude, distanceExpr, cursor))
			.and(categoryScore.eq(cursor.categoryMatch()))
			.and(addressScore.lt(cursor.addressMatch())));

		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.eq(cursor.nameSimilarity()))
			.and(distanceComparable(latitude, longitude, distanceExpr, cursor))
			.and(categoryScore.eq(cursor.categoryMatch()))
			.and(addressScore.eq(cursor.addressMatch()))
			.and(r.updatedAt.lt(cursor.updatedAt())));

		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.eq(cursor.nameSimilarity()))
			.and(distanceComparable(latitude, longitude, distanceExpr, cursor))
			.and(categoryScore.eq(cursor.categoryMatch()))
			.and(addressScore.eq(cursor.addressMatch()))
			.and(r.updatedAt.eq(cursor.updatedAt()))
			.and(r.id.lt(cursor.id())));

		return cond;
	}

	private BooleanExpression cursorCondition(SearchCursor cursor, QRestaurant r, String keywordLower,
		Double latitude, Double longitude, NumberExpression<Integer> categoryScore) {
		if (cursor == null) {
			return null;
		}

		NumberExpression<Integer> nameExactScore = nameExactScore(r, keywordLower);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, keywordLower);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> addressScore = addressMatchScore(r, keywordLower);

		BooleanExpression cond = null;
		cond = or(cond, nameExactScore.lt(cursor.nameExact()));
		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.lt(cursor.nameSimilarity())));

		if (latitude != null && longitude != null) {
			NumberExpression<Double> distanceSortExpr = Expressions.numberTemplate(Double.class,
				"COALESCE({0}, {1})", distanceExpr, 1e15);
			double cursorDist = cursor.distanceMeters() == null ? 1e15 : cursor.distanceMeters();
			cond = or(cond, nameExactScore.eq(cursor.nameExact())
				.and(nameSimilarity.eq(cursor.nameSimilarity()))
				.and(distanceSortExpr.gt(cursorDist)));
		}

		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.eq(cursor.nameSimilarity()))
			.and(distanceComparable(latitude, longitude, distanceExpr, cursor))
			.and(categoryScore.lt(cursor.categoryMatch())));

		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.eq(cursor.nameSimilarity()))
			.and(distanceComparable(latitude, longitude, distanceExpr, cursor))
			.and(categoryScore.eq(cursor.categoryMatch()))
			.and(addressScore.lt(cursor.addressMatch())));

		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.eq(cursor.nameSimilarity()))
			.and(distanceComparable(latitude, longitude, distanceExpr, cursor))
			.and(categoryScore.eq(cursor.categoryMatch()))
			.and(addressScore.eq(cursor.addressMatch()))
			.and(r.updatedAt.lt(cursor.updatedAt())));

		cond = or(cond, nameExactScore.eq(cursor.nameExact())
			.and(nameSimilarity.eq(cursor.nameSimilarity()))
			.and(distanceComparable(latitude, longitude, distanceExpr, cursor))
			.and(categoryScore.eq(cursor.categoryMatch()))
			.and(addressScore.eq(cursor.addressMatch()))
			.and(r.updatedAt.eq(cursor.updatedAt()))
			.and(r.id.lt(cursor.id())));

		return cond;
	}

	private BooleanExpression distanceComparable(Double latitude, Double longitude,
		NumberExpression<Double> distanceExpr, SearchCursor cursor) {
		if (latitude == null || longitude == null) {
			return Expressions.booleanTemplate("true");
		}
		NumberExpression<Double> distanceSortExpr = Expressions.numberTemplate(Double.class,
			"COALESCE({0}, {1})", distanceExpr, 1e15);
		double cursorDist = cursor.distanceMeters() == null ? 1e15 : cursor.distanceMeters();
		return distanceSortExpr.eq(cursorDist);
	}

	private BooleanExpression or(BooleanExpression base, BooleanExpression next) {
		return base == null ? next : base.or(next);
	}
}
