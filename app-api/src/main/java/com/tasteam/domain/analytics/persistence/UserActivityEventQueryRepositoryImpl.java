package com.tasteam.domain.analytics.persistence;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.tasteam.domain.admin.dto.request.AdminUserActivityEventSearchCondition;
import com.tasteam.domain.common.repository.QueryDslSupport;

@Repository
public class UserActivityEventQueryRepositoryImpl extends QueryDslSupport
	implements UserActivityEventQueryRepository {

	public UserActivityEventQueryRepositoryImpl() {
		super(UserActivityEventEntity.class);
	}

	@Override
	public Page<UserActivityEventEntity> findByCondition(
		AdminUserActivityEventSearchCondition condition, Pageable pageable) {

		QUserActivityEventEntity e = QUserActivityEventEntity.userActivityEventEntity;

		JPAQuery<UserActivityEventEntity> contentQuery = getQueryFactory()
			.selectFrom(e)
			.where(
				eqEventName(e, condition.eventName()),
				eqSource(e, condition.source()),
				eqMemberId(e, condition.memberId()),
				eqPlatform(e, condition.platform()),
				goeOccurredAt(e, condition.occurredAtFrom()),
				loeOccurredAt(e, condition.occurredAtTo()));

		JPAQuery<Long> countQuery = getQueryFactory()
			.select(e.id.count())
			.from(e)
			.where(
				eqEventName(e, condition.eventName()),
				eqSource(e, condition.source()),
				eqMemberId(e, condition.memberId()),
				eqPlatform(e, condition.platform()),
				goeOccurredAt(e, condition.occurredAtFrom()),
				loeOccurredAt(e, condition.occurredAtTo()));

		return applyPagination(pageable, contentQuery, countQuery);
	}

	private BooleanExpression eqEventName(QUserActivityEventEntity e, String eventName) {
		return eventName != null ? e.eventName.eq(eventName) : null;
	}

	private BooleanExpression eqSource(QUserActivityEventEntity e, String source) {
		return source != null ? e.source.eq(source) : null;
	}

	private BooleanExpression eqMemberId(QUserActivityEventEntity e, Long memberId) {
		return memberId != null ? e.memberId.eq(memberId) : null;
	}

	private BooleanExpression eqPlatform(QUserActivityEventEntity e, String platform) {
		return platform != null ? e.platform.eq(platform) : null;
	}

	private BooleanExpression goeOccurredAt(QUserActivityEventEntity e, Instant occurredAtFrom) {
		return occurredAtFrom != null ? e.occurredAt.goe(occurredAtFrom) : null;
	}

	private BooleanExpression loeOccurredAt(QUserActivityEventEntity e, Instant occurredAtTo) {
		return occurredAtTo != null ? e.occurredAt.loe(occurredAtTo) : null;
	}
}
