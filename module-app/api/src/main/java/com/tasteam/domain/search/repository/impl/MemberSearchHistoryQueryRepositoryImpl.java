package com.tasteam.domain.search.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.domain.search.entity.QMemberSearchHistory;
import com.tasteam.domain.search.repository.MemberSearchHistoryQueryRepository;

@Repository
public class MemberSearchHistoryQueryRepositoryImpl extends QueryDslSupport
	implements MemberSearchHistoryQueryRepository {

	public MemberSearchHistoryQueryRepositoryImpl() {
		super(MemberSearchHistory.class);
	}

	@Override
	public List<MemberSearchHistory> findRecentSearches(Long memberId, SearchCursor cursor, int size) {
		QMemberSearchHistory msh = QMemberSearchHistory.memberSearchHistory;

		return getQueryFactory()
			.selectFrom(msh)
			.where(
				msh.memberId.eq(memberId),
				msh.deletedAt.isNull(),
				cursorCondition(cursor, msh))
			.orderBy(msh.updatedAt.desc(), msh.id.desc())
			.limit(size)
			.fetch();
	}

	private BooleanExpression cursorCondition(SearchCursor cursor, QMemberSearchHistory msh) {
		if (cursor == null) {
			return null;
		}
		return msh.updatedAt.lt(cursor.updatedAt())
			.or(msh.updatedAt.eq(cursor.updatedAt()).and(msh.id.lt(cursor.id())));
	}
}
