package com.tasteam.domain.search.repository;

import java.util.List;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.type.GroupStatus;

public interface SearchGroupRepository {

	List<Group> searchByKeyword(String keyword, GroupStatus status, int pageSize);
}
