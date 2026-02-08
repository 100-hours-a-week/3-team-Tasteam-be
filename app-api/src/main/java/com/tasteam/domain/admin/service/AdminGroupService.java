package com.tasteam.domain.admin.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.request.AdminGroupCreateRequest;
import com.tasteam.domain.admin.dto.response.AdminGroupListItem;
import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FileErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminGroupService {

	private final GroupRepository groupRepository;
	private final ImageRepository imageRepository;
	private final DomainImageRepository domainImageRepository;
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

		Image image = imageRepository.findByFileUuid(parseUuid(logoImageFileUuid))
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		if (image.getStatus() == ImageStatus.DELETED) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		if (image.getStatus() != ImageStatus.PENDING
			&& domainImageRepository.findByDomainTypeAndDomainIdAndImage(DomainType.GROUP, group.getId(), image)
				.isEmpty()) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		domainImageRepository.deleteAllByDomainTypeAndDomainId(DomainType.GROUP, group.getId());
		domainImageRepository.save(DomainImage.create(DomainType.GROUP, group.getId(), image, 0));

		if (image.getStatus() == ImageStatus.PENDING) {
			image.activate();
		}
	}

	private Map<Long, String> resolveLogoImageUrlMap(List<Long> groupIds) {
		if (groupIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, List<DomainImageItem>> images = fileService.getDomainImageUrls(DomainType.GROUP, groupIds);
		return images.entrySet().stream()
			.filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getFirst().url()));
	}

	private UUID parseUuid(String fileUuid) {
		try {
			return UUID.fromString(fileUuid);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "fileUuid 형식이 올바르지 않습니다");
		}
	}
}
