package com.tasteam.domain.group.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tasteam.config.TestCurrentUserContext;
import com.tasteam.config.annotation.IntegrationTest;
import com.tasteam.config.annotation.WithCustomMockUser;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.MemberRole;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.entity.SubgroupMember;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;

@IntegrationTest
@Transactional
@DisplayName("Group/Subgroup API Smoke 통합 테스트")
class GroupSubgroupApiSmokeIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TestCurrentUserContext testCurrentUserContext;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private SubgroupRepository subgroupRepository;

	@Autowired
	private SubgroupMemberRepository subgroupMemberRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	private Member member;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(MemberFixture.create(
			"smoke-" + System.nanoTime() + "@example.com",
			"smoke-user"));
		testCurrentUserContext.setCurrentUserId(member.getId());
	}

	@AfterEach
	void tearDown() {
		testCurrentUserContext.clear();
	}

	@Test
	@WithCustomMockUser(role = MemberRole.USER)
	@DisplayName("그룹/하위그룹 생성-수정(이미지교체)-삭제 흐름이 API 레벨에서 정상 동작한다")
	void groupSubgroupCrudSmokeFlow() throws Exception {
		UUID groupImage1 = UUID.randomUUID();
		UUID groupImage2 = UUID.randomUUID();
		UUID subgroupImage1 = UUID.randomUUID();
		UUID subgroupImage2 = UUID.randomUUID();
		seedPendingImage(FilePurpose.GROUP_IMAGE, groupImage1, "group-1.png");
		seedPendingImage(FilePurpose.GROUP_IMAGE, groupImage2, "group-2.png");
		seedPendingImage(FilePurpose.PROFILE_IMAGE, subgroupImage1, "subgroup-1.png");
		seedPendingImage(FilePurpose.PROFILE_IMAGE, subgroupImage2, "subgroup-2.png");

		long groupId = createPasswordGroup(groupImage1);
		assertActiveAndLinked(DomainType.GROUP, groupId, groupImage1);

		replaceGroupImage(groupId, groupImage2);
		assertActiveAndLinked(DomainType.GROUP, groupId, groupImage2);

		joinGroupByPassword(groupId, "123456");
		assertThat(groupMemberRepository.findByGroupIdAndMember_IdAndDeletedAtIsNull(groupId, member.getId()))
			.isPresent();

		long subgroupId = createSubgroup(groupId, subgroupImage1);
		assertActiveAndLinked(DomainType.SUBGROUP, subgroupId, subgroupImage1);

		replaceSubgroupImage(groupId, subgroupId, subgroupImage2);
		assertActiveAndLinked(DomainType.SUBGROUP, subgroupId, subgroupImage2);

		withdrawSubgroup(subgroupId);
		SubgroupMember subgroupMember = subgroupMemberRepository
			.findBySubgroupIdAndMember_Id(subgroupId, member.getId())
			.orElseThrow();
		assertThat(subgroupMember.getDeletedAt()).isNotNull();
		Subgroup subgroup = subgroupRepository.findById(subgroupId).orElseThrow();
		assertThat(subgroup.getMemberCount()).isZero();

		deleteGroup(groupId);
		Group group = groupRepository.findById(groupId).orElseThrow();
		assertThat(group.getDeletedAt()).isNotNull();
	}

	@Test
	@WithCustomMockUser(role = MemberRole.USER)
	@DisplayName("patch null 로 그룹/하위그룹 이미지 연결을 제거할 수 있다")
	void canRemoveGroupAndSubgroupImageByPatchNull() throws Exception {
		UUID groupImage = UUID.randomUUID();
		UUID subgroupImage = UUID.randomUUID();
		seedPendingImage(FilePurpose.GROUP_IMAGE, groupImage, "group-remove.png");
		seedPendingImage(FilePurpose.PROFILE_IMAGE, subgroupImage, "subgroup-remove.png");

		long groupId = createPasswordGroup(groupImage);
		joinGroupByPassword(groupId, "123456");
		long subgroupId = createSubgroup(groupId, subgroupImage);

		ObjectNode clearGroupBody = objectMapper.createObjectNode();
		clearGroupBody.putNull("logoImageFileUuid");
		mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(clearGroupBody)))
			.andExpect(status().isOk());

		ObjectNode clearSubgroupBody = objectMapper.createObjectNode();
		clearSubgroupBody.putNull("profileImageFileUuid");
		mockMvc.perform(patch("/api/v1/groups/{groupId}/subgroups/{subgroupId}", groupId, subgroupId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(clearSubgroupBody)))
			.andExpect(status().isOk());

		assertThat(domainImageRepository.findAllByDomainTypeAndDomainId(DomainType.GROUP, groupId)).isEmpty();
		assertThat(domainImageRepository.findAllByDomainTypeAndDomainId(DomainType.SUBGROUP, subgroupId)).isEmpty();
	}

	private void seedPendingImage(FilePurpose purpose, UUID fileUuid, String fileName) {
		String storageKey = "uploads/smoke/" + fileUuid + "-" + fileName;
		imageRepository.save(ImageFixture.create(purpose, storageKey, fileUuid, fileName));
	}

	private long createPasswordGroup(UUID logoFileUuid) throws Exception {
		ObjectNode location = objectMapper.createObjectNode();
		location.put("latitude", 37.5);
		location.put("longitude", 127.0);

		ObjectNode body = objectMapper.createObjectNode();
		body.put("name", "smoke-group-" + System.nanoTime());
		body.put("logoImageFileUuid", logoFileUuid.toString());
		body.put("type", "UNOFFICIAL");
		body.put("address", "서울시 강남구 테스트로 1");
		body.putNull("detailAddress");
		body.set("location", location);
		body.put("joinType", "PASSWORD");
		body.putNull("emailDomain");
		body.put("code", "123456");

		MvcResult result = mockMvc.perform(post("/api/v1/groups")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		return json.path("data").path("id").asLong();
	}

	private void replaceGroupImage(long groupId, UUID replacementImageUuid) throws Exception {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("logoImageFileUuid", replacementImageUuid.toString());

		mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk());
	}

	private void joinGroupByPassword(long groupId, String code) throws Exception {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("code", code);

		mockMvc.perform(post("/api/v1/groups/{groupId}/password-authentications", groupId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated());
	}

	private long createSubgroup(long groupId, UUID profileImageUuid) throws Exception {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("name", "smoke-subgroup-" + System.nanoTime());
		body.put("description", "smoke description");
		body.put("profileImageFileUuid", profileImageUuid.toString());
		body.put("joinType", "OPEN");
		body.putNull("password");

		MvcResult result = mockMvc.perform(post("/api/v1/groups/{groupId}/subgroups", groupId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		return json.path("data").path("data").path("id").asLong();
	}

	private void replaceSubgroupImage(long groupId, long subgroupId, UUID replacementImageUuid) throws Exception {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("profileImageFileUuid", replacementImageUuid.toString());

		mockMvc.perform(patch("/api/v1/groups/{groupId}/subgroups/{subgroupId}", groupId, subgroupId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk());
	}

	private void withdrawSubgroup(long subgroupId) throws Exception {
		mockMvc.perform(delete("/api/v1/subgroups/{subgroupId}/members/me", subgroupId))
			.andExpect(status().isNoContent());
	}

	private void deleteGroup(long groupId) throws Exception {
		mockMvc.perform(delete("/api/v1/groups/{groupId}", groupId))
			.andExpect(status().isOk());
	}

	private void assertActiveAndLinked(DomainType domainType, long domainId, UUID expectedFileUuid) {
		Image image = imageRepository.findByFileUuid(expectedFileUuid).orElseThrow();
		assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);

		List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainId(domainType, domainId);
		assertThat(links).hasSize(1);
		assertThat(links.getFirst().getImage().getFileUuid()).isEqualTo(expectedFileUuid);
	}
}
