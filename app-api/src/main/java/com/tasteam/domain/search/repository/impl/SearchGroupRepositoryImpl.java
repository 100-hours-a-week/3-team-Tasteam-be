package com.tasteam.domain.search.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupQueryRepository;
import com.tasteam.domain.group.repository.projection.GroupMemberCountProjection;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.search.repository.SearchGroupRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SearchGroupRepositoryImpl implements SearchGroupRepository {

	private final GroupQueryRepository groupQueryRepository;
	private final GroupMemberRepository groupMemberRepository;

	@Override
	public List<Group> searchByKeyword(String keyword, GroupStatus status, int pageSize) {
		return groupQueryRepository.searchByKeyword(keyword, status, pageSize);
	}

	@Override
	public List<GroupMemberCountProjection> findMemberCounts(List<Long> groupIds) {
		return groupMemberRepository.findMemberCounts(groupIds);
	}
}
