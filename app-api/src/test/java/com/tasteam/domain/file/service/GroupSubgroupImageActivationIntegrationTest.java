package com.tasteam.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.node.TextNode;
import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupUpdateRequest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.service.GroupService;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.service.SubgroupService;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.GroupRequestFixture;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.fixture.SubgroupRequestFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.FileErrorCode;

@ServiceIntegrationTest
@Transactional
class GroupSubgroupImageActivationIntegrationTest {

	private static final String MISSING_FILE_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

	@Autowired
	private GroupService groupService;

	@Autowired
	private SubgroupService subgroupService;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SubgroupRepository subgroupRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	@Nested
	@DisplayName("Group 이미지 상태 전환")
	class GroupImageActivation {

		@Test
		@DisplayName("그룹 생성에서 PENDING 이미지를 연결하면 ACTIVE로 전환된다")
		void createGroupActivatesPendingImage() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(ImageFixture.create(FilePurpose.GROUP_IMAGE, "uploads/group/image/group-create.png",
				fileUuid, "group-create.png"));

			GroupCreateRequest request = GroupRequestFixture.createPasswordGroupRequestWithLogo(
				"group-" + System.nanoTime(),
				fileUuid.toString(),
				"123456");

			GroupCreateResponse response = groupService.createGroup(request);

			Image updatedImage = imageRepository.findByFileUuid(fileUuid).orElseThrow();
			assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.GROUP,
				List.of(response.id()));
			assertThat(links).hasSize(1);
		}

		@Test
		@DisplayName("그룹 수정에서 PENDING 이미지를 연결하면 ACTIVE로 전환된다")
		void updateGroupActivatesPendingImage() {
			Group group = groupRepository.save(GroupFixture.create("update-group-" + System.nanoTime(), "서울시 강남구"));
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(ImageFixture.create(FilePurpose.GROUP_IMAGE, "uploads/group/image/group-update.png",
				fileUuid, "group-update.png"));

			groupService.updateGroup(
				group.getId(),
				new GroupUpdateRequest(null, null, null, null, null, TextNode.valueOf(fileUuid.toString())));

			Image updatedImage = imageRepository.findByFileUuid(fileUuid).orElseThrow();
			assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.GROUP,
				List.of(group.getId()));
			assertThat(links).hasSize(1);
		}

		@Test
		@DisplayName("그룹 생성에서 존재하지 않는 이미지를 지정하면 실패한다")
		void createGroupWithMissingImageFails() {
			GroupCreateRequest request = GroupRequestFixture.createPasswordGroupRequestWithLogo(
				"group-missing-image",
				MISSING_FILE_UUID,
				"123456");

			assertThatThrownBy(() -> groupService.createGroup(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(FileErrorCode.FILE_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("Subgroup 이미지 상태 전환")
	class SubgroupImageActivation {

		@Test
		@DisplayName("하위그룹 생성에서 PENDING 이미지를 연결하면 ACTIVE로 전환된다")
		void createSubgroupActivatesPendingImage() {
			Member member = memberRepository
				.save(MemberFixture.create("subgroup-create@example.com", "subgroup-create"));
			Group group = groupRepository.save(GroupFixture.create("subgroup-group-" + System.nanoTime(), "서울시 강남구"));
			groupMemberRepository.save(GroupMember.create(group.getId(), member));

			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(ImageFixture.create(FilePurpose.PROFILE_IMAGE,
				"uploads/profile/image/subgroup-create.png", fileUuid, "subgroup-create.png"));

			var request = SubgroupRequestFixture.createRequestWithImage(
				"subgroup-" + System.nanoTime(), fileUuid.toString());

			SubgroupCreateResponse response = subgroupService.createSubgroup(group.getId(), member.getId(), request);

			Image updatedImage = imageRepository.findByFileUuid(fileUuid).orElseThrow();
			assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.SUBGROUP,
				List.of(response.data().id()));
			assertThat(links).hasSize(1);
		}

		@Test
		@DisplayName("하위그룹 수정에서 PENDING 이미지를 연결하면 ACTIVE로 전환된다")
		void updateSubgroupActivatesPendingImage() {
			Member member = memberRepository
				.save(MemberFixture.create("subgroup-update@example.com", "subgroup-update"));
			Group group = groupRepository
				.save(GroupFixture.create("subgroup-update-group-" + System.nanoTime(), "서울시 강남구"));
			groupMemberRepository.save(GroupMember.create(group.getId(), member));

			SubgroupCreateResponse created = subgroupService.createSubgroup(
				group.getId(),
				member.getId(),
				SubgroupRequestFixture.createRequest("subgroup-update-" + System.nanoTime()));
			Subgroup subgroup = subgroupRepository.findById(created.data().id()).orElseThrow();

			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(ImageFixture.create(FilePurpose.PROFILE_IMAGE,
				"uploads/profile/image/subgroup-update.png", fileUuid, "subgroup-update.png"));

			subgroupService.updateSubgroup(
				group.getId(),
				subgroup.getId(),
				member.getId(),
				new SubgroupUpdateRequest(null, null, TextNode.valueOf(fileUuid.toString())));

			Image updatedImage = imageRepository.findByFileUuid(fileUuid).orElseThrow();
			assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.SUBGROUP,
				List.of(subgroup.getId()));
			assertThat(links).hasSize(1);
		}

		@Test
		@DisplayName("하위그룹 생성에서 존재하지 않는 이미지를 지정하면 실패한다")
		void createSubgroupWithMissingImageFails() {
			Member member = memberRepository
				.save(MemberFixture.create("subgroup-missing@example.com", "subgroup-missing"));
			Group group = groupRepository.save(GroupFixture.create("subgroup-missing-group", "서울시 강남구"));
			groupMemberRepository.save(GroupMember.create(group.getId(), member));

			var request = SubgroupRequestFixture.createRequestWithImage("subgroup-missing", MISSING_FILE_UUID);

			assertThatThrownBy(() -> subgroupService.createSubgroup(group.getId(), member.getId(), request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(FileErrorCode.FILE_NOT_FOUND.name());
		}
	}
}
