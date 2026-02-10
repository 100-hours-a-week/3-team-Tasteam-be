package com.tasteam.domain.group.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.dto.GroupMemberListItem;
import com.tasteam.domain.group.dto.GroupMemberListResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupUpdateRequest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupAuthCode;
import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.group.event.GroupEventPublisher;
import com.tasteam.domain.group.repository.GroupAuthCodeRepository;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.group.type.GroupType;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.entity.SubgroupMember;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FileErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.notification.email.EmailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final MemberRepository memberRepository;
	private final GroupAuthCodeRepository groupAuthCodeRepository;
	private final EmailSender emailSender;
	private final SubgroupMemberRepository subgroupMemberRepository;
	private final SubgroupRepository subgroupRepository;
	private final ImageRepository imageRepository;
	private final DomainImageRepository domainImageRepository;
	private final FileService fileService;
	private final PasswordEncoder passwordEncoder;
	private final GroupEventPublisher groupEventPublisher;

	@Transactional
	public GroupCreateResponse createGroup(GroupCreateRequest request) {
		if (groupRepository.existsByNameAndDeletedAtIsNull(request.name())) {
			throw new BusinessException(GroupErrorCode.ALREADY_EXISTS);
		}

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

		Group savedGroup = groupRepository.save(group);

		if (request.logoImageFileUuid() != null) {
			saveDomainImage(DomainType.GROUP, savedGroup.getId(), request.logoImageFileUuid());
		}

		if (savedGroup.getJoinType() == GroupJoinType.PASSWORD) {
			groupAuthCodeRepository.save(GroupAuthCode.builder()
				.groupId(savedGroup.getId())
				.code(passwordEncoder.encode(request.code()))
				.email(null)
				.expiresAt(null)
				.build());
		}
		return GroupCreateResponse.from(savedGroup);
	}

	@Transactional(readOnly = true)
	public GroupGetResponse getGroup(Long groupId) {
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		String logoImageUrl = resolveLogoImageUrl(group.getId());

		return new GroupGetResponse(
			new GroupGetResponse.GroupData(
				group.getId(),
				group.getName(),
				logoImageUrl,
				group.getAddress(),
				group.getDetailAddress(),
				group.getEmailDomain(),
				groupMemberRepository.countByGroupIdAndDeletedAtIsNull(group.getId()),
				group.getStatus().name(),
				group.getCreatedAt(),
				group.getUpdatedAt()));
	}

	@Transactional
	public void updateGroup(Long groupId, GroupUpdateRequest request) {
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		applyStringIfPresent(request.name(), group::updateName, false);
		applyStringIfPresent(request.address(), group::updateAddress, false);
		applyStringIfPresent(request.detailAddress(), group::updateDetailAddress, true);
		applyStringIfPresent(request.emailDomain(), group::updateEmailDomain, true);
		applyLogoImageUrl(request.logoImageFileUuid(), group);
		applyStatusIfPresent(request.status(), group);
	}

	@Transactional
	public GroupEmailVerificationResponse sendGroupEmailVerification(Long groupId, String email) {
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		if (group.getJoinType() != GroupJoinType.EMAIL) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		if (group.getEmailDomain() == null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		validateEmailDomain(email, group.getEmailDomain());

		Instant now = Instant.now();
		if (groupAuthCodeRepository.existsByGroupIdAndExpiresAtAfterAndVerifiedAtIsNull(groupId, now)) {
			throw new BusinessException(GroupErrorCode.EMAIL_ALREADY_EXISTS);
		}

		String code = generateVerificationCode();
		Instant expiresAt = now.plus(Duration.ofMinutes(10));

		GroupAuthCode authCode = groupAuthCodeRepository.save(GroupAuthCode.builder()
			.groupId(groupId)
			.code(code)
			.email(email)
			.expiresAt(expiresAt)
			.build());

		emailSender.sendGroupJoinVerification(email, code, expiresAt);

		return GroupEmailVerificationResponse.from(authCode);
	}

	@Transactional
	public GroupEmailAuthenticationResponse authenticateGroupByEmail(Long groupId, Long memberId, String code) {
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		if (group.getJoinType() != GroupJoinType.EMAIL) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		GroupAuthCode authCode = groupAuthCodeRepository
			.findByGroupIdAndCodeAndExpiresAtAfterAndVerifiedAtIsNull(groupId, code, Instant.now())
			.orElseThrow(() -> new BusinessException(GroupErrorCode.EMAIL_CODE_MISMATCH));

		GroupMember groupMember = groupMemberRepository
			.findByGroupIdAndMember_Id(groupId, memberId)
			.orElse(null);

		if (groupMember == null) {
			groupMember = groupMemberRepository.save(GroupMember.create(
				groupId,
				memberRepository.findByIdAndDeletedAtIsNull(memberId)
					.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND))));
		} else if (groupMember.getDeletedAt() != null) {
			groupMember.restore();
		}

		authCode.verify(Instant.now());

		groupEventPublisher.publishMemberJoined(groupId, memberId, group.getName(), groupMember.getCreatedAt());

		return new GroupEmailAuthenticationResponse(
			true,
			groupMember.getCreatedAt());
	}

	@Transactional
	public GroupPasswordAuthenticationResponse authenticateGroupByPassword(Long groupId, Long memberId, String code) {
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		if (group.getJoinType() != GroupJoinType.PASSWORD || group.getType() != GroupType.UNOFFICIAL) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		if (group.getStatus() != GroupStatus.ACTIVE) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		GroupAuthCode authCode = groupAuthCodeRepository.findByGroupId(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_PASSWORD_MISMATCH));

		if (!passwordEncoder.matches(code, authCode.getCode())) {
			throw new BusinessException(GroupErrorCode.GROUP_PASSWORD_MISMATCH);
		}

		GroupMember groupMember = groupMemberRepository
			.findByGroupIdAndMember_Id(groupId, memberId)
			.orElse(null);

		if (groupMember == null) {
			groupMember = groupMemberRepository.save(GroupMember.create(
				groupId,
				memberRepository.findByIdAndDeletedAtIsNull(memberId)
					.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND))));
		} else if (groupMember.getDeletedAt() != null) {
			groupMember.restore();
		}

		groupEventPublisher.publishMemberJoined(groupId, memberId, group.getName(), groupMember.getCreatedAt());

		return new GroupPasswordAuthenticationResponse(
			true,
			groupMember.getCreatedAt());
	}

	@Transactional
	public void deleteGroup(Long groupId) {
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
		group.delete(Instant.now());
	}

	@Transactional
	public void withdrawGroup(Long groupId, Long memberId) {
		Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
		GroupMember groupMember = groupMemberRepository.findByGroupIdAndMember_Id(
			group.getId(),
			memberId).orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
		if (groupMember.getDeletedAt() != null) {
			return;
		}
		groupMember.softDelete(Instant.now());

		List<SubgroupMember> subgroupMembers = subgroupMemberRepository.findActiveMembersByMemberAndGroup(
			groupId,
			memberId);
		if (subgroupMembers.isEmpty()) {
			return;
		}

		List<Long> subgroupIds = subgroupMembers.stream()
			.map(SubgroupMember::getSubgroupId)
			.toList();

		Map<Long, Subgroup> subgroupMap = subgroupRepository.findAllById(subgroupIds).stream()
			.collect(Collectors.toMap(Subgroup::getId, Function.identity()));

		Instant now = Instant.now();
		for (SubgroupMember subgroupMember : subgroupMembers) {
			subgroupMember.softDelete(now);
			Subgroup subgroup = subgroupMap.get(subgroupMember.getSubgroupId());
			if (subgroup != null) {
				subgroup.decreaseMemberCount();
			}
		}
	}

	@Transactional(readOnly = true)
	public GroupMemberListResponse getGroupMembers(Long groupId, String cursor, Integer size) {
		groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));

		int resolvedSize = resolveSize(size);
		Long cursorId = parseCursor(cursor);

		List<GroupMemberListItem> items = groupMemberRepository.findGroupMembers(
			groupId,
			cursorId,
			PageRequest.of(0, resolvedSize + 1));

		boolean hasNext = items.size() > resolvedSize;
		if (hasNext) {
			items = items.subList(0, resolvedSize);
		}

		String nextCursor = hasNext ? String.valueOf(items.get(items.size() - 1).cursorId()) : null;

		return new GroupMemberListResponse(
			items,
			new GroupMemberListResponse.PageInfo(
				nextCursor,
				resolvedSize,
				hasNext));
	}

	@Transactional
	public void deleteGroupMember(Long groupId, Long userId) {
		groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
		memberRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		GroupMember groupMember = groupMemberRepository.findByGroupIdAndMember_IdAndDeletedAtIsNull(
			groupId,
			userId).orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
		groupMember.softDelete(Instant.now());
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

	private void applyStringIfPresent(JsonNode node, Consumer<String> updater, boolean nullable) {
		if (node == null) {
			return;
		}
		if (node.isNull()) {
			if (!nullable) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			updater.accept(null);
			return;
		}
		if (!node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		updater.accept(node.asText());
	}

	private void applyStatusIfPresent(JsonNode node, Group group) {
		if (node == null) {
			return;
		}
		if (node.isNull() || !node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		try {
			GroupStatus status = GroupStatus.valueOf(node.asText());
			group.updateStatus(status);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private void applyLogoImageUrl(JsonNode node, Group group) {
		if (node == null) {
			return;
		}
		if (node.isNull()) {
			domainImageRepository.deleteAllByDomainTypeAndDomainId(DomainType.GROUP, group.getId());
			return;
		}
		if (!node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		saveDomainImage(DomainType.GROUP, group.getId(), node.asText());
	}

	private Point toPoint(GroupCreateRequest.Location location) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return geometryFactory.createPoint(new Coordinate(location.longitude(), location.latitude()));
	}

	private void validateEmailDomain(String email, String domain) {
		int atIndex = email.lastIndexOf('@');
		if (atIndex < 0 || atIndex == email.length() - 1) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		String emailDomain = email.substring(atIndex + 1);
		if (!emailDomain.equalsIgnoreCase(domain)) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private String generateVerificationCode() {
		int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
		return String.valueOf(code);
	}

	private int resolveSize(Integer size) {
		if (size == null) {
			return 10;
		}
		if (size < 1 || size > 100) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return size;
	}

	private Long parseCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(cursor);
		} catch (NumberFormatException e) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private void saveDomainImage(DomainType domainType, Long domainId, String fileUuid) {
		Image image = imageRepository.findByFileUuid(parseUuid(fileUuid))
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		if (image.getStatus() == ImageStatus.DELETED) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		if (image.getStatus() != ImageStatus.PENDING
			&& domainImageRepository.findByDomainTypeAndDomainIdAndImage(domainType, domainId, image).isEmpty()) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		domainImageRepository.deleteAllByDomainTypeAndDomainId(domainType, domainId);
		domainImageRepository.save(DomainImage.create(domainType, domainId, image, 0));

		if (image.getStatus() == ImageStatus.PENDING) {
			image.activate();
			imageRepository.save(image);
		}
	}

	private java.util.UUID parseUuid(String fileUuid) {
		try {
			return java.util.UUID.fromString(fileUuid);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "fileUuid 형식이 올바르지 않습니다");
		}
	}

	private String resolveLogoImageUrl(Long groupId) {
		Map<Long, List<DomainImageItem>> images = fileService.getDomainImageUrls(
			DomainType.GROUP,
			List.of(groupId));

		List<DomainImageItem> groupImages = images.get(groupId);
		if (groupImages == null || groupImages.isEmpty()) {
			return null;
		}
		return groupImages.getFirst().url();
	}
}
