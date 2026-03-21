package com.tasteam.domain.group.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.group.type.GroupType;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupCreateService {

	private final GroupRepository groupRepository;

	public Group create(GroupCreateRequest request) {
		validateCreateRequest(request);

		Group group = Group.builder()
			.name(request.name())
			.type(request.type())
			.address(request.address())
			.detailAddress(request.detailAddress())
			.location(toPoint(request.location()))
			.joinType(request.joinType())
			.emailDomain(request.emailDomain())
			.status(GroupStatus.ACTIVE)
			.build();

		return groupRepository.save(group);
	}

	private void validateCreateRequest(GroupCreateRequest request) {
		if (request.type() == GroupType.OFFICIAL) {
			if (request.joinType() != GroupJoinType.EMAIL) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			if (request.emailDomain() == null || request.emailDomain().isBlank()) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			if (request.code() != null) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			return;
		}

		if (request.type() == GroupType.UNOFFICIAL) {
			if (request.joinType() != GroupJoinType.PASSWORD) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			if (request.emailDomain() != null) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			if (request.code() == null || request.code().isBlank()) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
		}
	}

	private Point toPoint(GroupCreateRequest.Location location) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return geometryFactory.createPoint(new Coordinate(location.longitude(), location.latitude()));
	}
}
