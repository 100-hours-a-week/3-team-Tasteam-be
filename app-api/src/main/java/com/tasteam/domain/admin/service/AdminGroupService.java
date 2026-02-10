package com.tasteam.domain.admin.service;

import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.request.AdminGroupCreateRequest;
import com.tasteam.domain.admin.dto.response.AdminGroupListItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
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
	private final FileService fileService;

	@Transactional(readOnly = true)
	public Page<AdminGroupListItem> getGroups(Pageable pageable) {
		Page<Group> groups = groupRepository.findAll(pageable);

		List<Long> groupIds = groups.getContent().stream()
			.map(Group::getId)
			.toList();

		Map<Long, String> logoImageMap = resolveLogoImageUrlMap(groupIds);

		return groups.map(g -> new AdminGroupListItem(
			g.getId(),
			g.getName(),
			g.getType(),
			g.getAddress(),
			g.getJoinType(),
			logoImageMap.get(g.getId()),
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

		Group savedGroup = groupRepository.save(group);

		applyLogoImageIfPresent(savedGroup, request.logoImageFileUuid());

		return savedGroup.getId();
	}

	private void applyLogoImageIfPresent(Group group, String logoImageFileUuid) {
		if (logoImageFileUuid == null || logoImageFileUuid.isBlank()) {
			return;
		}
		fileService.replaceDomainImage(DomainType.GROUP, group.getId(), logoImageFileUuid);
	}

	private Map<Long, String> resolveLogoImageUrlMap(List<Long> groupIds) {
		if (groupIds.isEmpty()) {
			return Map.of();
		}
		return fileService.getPrimaryDomainImageUrlMap(DomainType.GROUP, groupIds);
	}
}
