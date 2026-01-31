package com.tasteam.domain.group.repository.impl;

import static com.tasteam.domain.group.entity.QGroup.group;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupQueryRepository;
import com.tasteam.domain.group.type.GroupStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GroupQueryRepositoryImpl implements GroupQueryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<Group> searchByKeyword(String keyword, GroupStatus status, int pageSize) {
		String pattern = "%" + keyword.toLowerCase() + "%";

		return queryFactory
			.selectFrom(group)
			.where(
				group.deletedAt.isNull(),
				group.status.eq(status),
				group.name.lower().like(pattern)
					.or(group.address.lower().like(pattern)))
			.orderBy(
				group.updatedAt.desc(),
				group.id.desc())
			.limit(pageSize)
			.fetch();
	}
}
