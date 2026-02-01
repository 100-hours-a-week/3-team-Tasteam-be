package com.tasteam.domain.admin.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.request.AdminGroupCreateRequest;
import com.tasteam.domain.admin.dto.response.AdminGroupListItem;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminGroupService {

	private final GroupRepository groupRepository;
	private final GeometryFactory geometryFactory;
	private final NaverGeocodingClient naverGeocodingClient;

	@Transactional(readOnly = true)
	public Page<AdminGroupListItem> getGroups(Pageable pageable) {
		Page<Group> groups = groupRepository.findAll(pageable);

		return groups.map(g -> new AdminGroupListItem(
			g.getId(),
			g.getName(),
			g.getType(),
			g.getAddress(),
			g.getJoinType(),
			g.getStatus(),
			g.getCreatedAt()));
	}

	@Transactional
	public Long createGroup(AdminGroupCreateRequest request) {
		GeocodingResult result = naverGeocodingClient.geocode(request.address());

		Coordinate coordinate = new Coordinate(result.longitude(), result.latitude());
		Point location = geometryFactory.createPoint(coordinate);

		Group group = Group.builder()
			.name(request.name())
			.type(request.type())
			.address(request.address())
			.detailAddress(request.detailAddress())
			.location(location)
			.joinType(request.joinType())
			.emailDomain(request.emailDomain())
			.status(GroupStatus.ACTIVE)
			.build();

		return groupRepository.save(group).getId();
	}
}
