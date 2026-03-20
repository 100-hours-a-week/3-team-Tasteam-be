package com.tasteam.domain.group.repository;

import java.util.List;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.type.GroupStatus;

public interface GroupQueryRepository {

	List<Group> searchByKeyword(String keyword, GroupStatus status, int pageSize);
}
