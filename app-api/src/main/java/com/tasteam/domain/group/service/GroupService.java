package com.tasteam.domain.group.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.global.exception.business.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

	private final GroupRepository groupRepository;

	@Transactional(readOnly = true)
	public GroupGetResponse getGroup(Long groupId) {
		return groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.map(GroupGetResponse::from)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
	}
}
