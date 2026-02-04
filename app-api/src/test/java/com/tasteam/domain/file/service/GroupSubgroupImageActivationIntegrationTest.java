package com.tasteam.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupType;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.service.SubgroupService;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.fixture.SubgroupRequestFixture;

@ServiceIntegrationTest
@Transactional
class GroupSubgroupImageActivationIntegrationTest {

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

			GroupCreateRequest request = new GroupCreateRequest(
				"group-" + System.nanoTime(),
				fileUuid.toString(),
				GroupType.UNOFFICIAL,
				"서울특별시 강남구",
				null,
				new GroupCreateRequest.Location(37.5, 127.0),
				GroupJoinType.PASSWORD,
				null,
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

			var request = SubgroupRequestFixture.createRequest(
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
	}
}
