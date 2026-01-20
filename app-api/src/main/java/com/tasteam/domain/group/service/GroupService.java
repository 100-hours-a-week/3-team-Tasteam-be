package com.tasteam.domain.group.service;

import com.tasteam.global.exception.code.GroupErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupJoinType;
import com.tasteam.domain.group.entity.GroupStatus;
import com.tasteam.domain.group.entity.GroupType;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

	private final GroupRepository groupRepository;

	@Transactional
	public GroupCreateResponse createGroup(GroupCreateRequest request) {
		if (groupRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
			throw new BusinessException(GroupErrorCode.ALREADY_EXISTS);
		}

		validateCreateRequest(request);

		Group group = Group.builder()
			.name(request.getName())
			.type(request.getType())
			.logoImageUrl(request.getLogoImageUrl())
			.address(request.getAddress())
			.detailAddress(request.getDetailAddress())
			.location(toPoint(request.getLocation()))
			.joinType(request.getJoinType())
			.emailDomain(request.getEmailDomain())
			.status(GroupStatus.ACTIVE)
			.build();

		Group savedGroup = groupRepository.save(group);
		return GroupCreateResponse.from(savedGroup);
	}

	@Transactional(readOnly = true)
	public GroupGetResponse getGroup(Long groupId) {
		return groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.map(GroupGetResponse::from)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
	}

	private void validateCreateRequest(GroupCreateRequest request) {
		if (request.getType() == GroupType.OFFICIAL) {
			if (request.getJoinType() != GroupJoinType.EMAIL) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			if (request.getEmailDomain() == null || request.getEmailDomain().isBlank()) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			return;
		}

		if (request.getType() == GroupType.UNOFFICIAL) {
			if (request.getJoinType() != GroupJoinType.PASSWORD) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			if (request.getEmailDomain() != null) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
		}
	}

	private Point toPoint(GroupCreateRequest.Location location) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return geometryFactory.createPoint(new Coordinate(location.getLongitude(), location.getLatitude()));
	}
}
