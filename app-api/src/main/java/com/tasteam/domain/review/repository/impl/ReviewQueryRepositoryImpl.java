package com.tasteam.domain.review.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.member.entity.QMember;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.review.dto.ReviewCursor;
import com.tasteam.domain.review.dto.ReviewMemberQueryDto;
import com.tasteam.domain.review.dto.ReviewQueryDto;
import com.tasteam.domain.review.entity.QReview;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.repository.ReviewQueryRepository;

@Repository
public class ReviewQueryRepositoryImpl extends QueryDslSupport implements ReviewQueryRepository {

	public ReviewQueryRepositoryImpl() {
		super(Review.class);
	}

	@Override
	public List<ReviewQueryDto> findRestaurantReviews(Long restaurantId, ReviewCursor cursor, int size) {
		QReview review = QReview.review;
		QMember member = QMember.member;

		return getQueryFactory()
			.select(Projections.constructor(
				ReviewQueryDto.class,
				review.id,
				member.id,
				member.nickname,
				review.content,
				review.isRecommended,
				review.createdAt))
			.from(review)
			.join(review.member, member)
			.where(
				review.restaurant.id.eq(restaurantId),
				review.deletedAt.isNull(),
				cursorCondition(cursor, review))
			.orderBy(review.createdAt.desc(), review.id.desc())
			.limit(size)
			.fetch();
	}

	@Override
	public List<ReviewQueryDto> findGroupReviews(Long groupId, ReviewCursor cursor, int size) {
		QReview review = QReview.review;
		QMember member = QMember.member;

		return getQueryFactory()
			.select(Projections.constructor(
				ReviewQueryDto.class,
				review.id,
				member.id,
				member.nickname,
				review.content,
				review.isRecommended,
				review.createdAt))
			.from(review)
			.join(review.member, member)
			.where(
				review.groupId.eq(groupId),
				review.deletedAt.isNull(),
				cursorCondition(cursor, review))
			.orderBy(review.createdAt.desc(), review.id.desc())
			.limit(size)
			.fetch();
	}

	@Override
	public List<ReviewMemberQueryDto> findMemberReviews(Long memberId, ReviewCursor cursor, int size) {
		QReview review = QReview.review;
		QRestaurant restaurant = QRestaurant.restaurant;

		return getQueryFactory()
			.select(Projections.constructor(
				ReviewMemberQueryDto.class,
				review.id,
				restaurant.name,
				restaurant.fullAddress,
				review.content,
				review.createdAt))
			.from(review)
			.join(review.restaurant, restaurant)
			.where(
				review.member.id.eq(memberId),
				review.deletedAt.isNull(),
				restaurant.deletedAt.isNull(),
				cursorCondition(cursor, review))
			.orderBy(review.createdAt.desc(), review.id.desc())
			.limit(size)
			.fetch();
	}

	private BooleanExpression cursorCondition(ReviewCursor cursor, QReview review) {
		if (cursor == null) {
			return null;
		}
		return review.createdAt.lt(cursor.createdAt())
			.or(review.createdAt.eq(cursor.createdAt()).and(review.id.lt(cursor.id())));
	}
}
