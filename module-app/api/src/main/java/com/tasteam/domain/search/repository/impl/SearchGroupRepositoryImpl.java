package com.tasteam.domain.search.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupQueryRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.search.repository.SearchGroupRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SearchGroupRepositoryImpl implements SearchGroupRepository {

	private final GroupQueryRepository groupQueryRepository;

	@Override
	public List<Group> searchByKeyword(String keyword, GroupStatus status, int pageSize) {
		return groupQueryRepository.searchByKeyword(keyword, status, pageSize);
	}
}
