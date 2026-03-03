package com.tasteam.domain.subgroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ServiceIntegrationTest;
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
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinRequest;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.entity.SubgroupMember;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.GroupMemberFixture;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.fixture.SubgroupFixture;
import com.tasteam.fixture.SubgroupMemberFixture;
import com.tasteam.fixture.SubgroupRequestFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.SearchErrorCode;

@ServiceIntegrationTest
@Transactional
@DisplayName("[통합](Subgroup) SubgroupFacade 통합 테스트")
class SubgroupFacadeIntegrationTest {

	private static final UUID IMAGE_UUID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID IMAGE_UUID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private SubgroupFacade subgroupFacade;

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

	@Autowired
	private ObjectMapper objectMapper;

	private Member member1;
	private Member member2;
	private Group group;

	@BeforeEach
	void setUp() {
		member1 = memberRepository.save(MemberFixture.create("member1@test.com", "회원1"));
		member2 = memberRepository.save(MemberFixture.create("member2@test.com", "회원2"));
		group = groupRepository.save(GroupFixture.create());
		groupMemberRepository.save(GroupMemberFixture.create(group.getId(), member1));
		groupMemberRepository.save(GroupMemberFixture.create(group.getId(), member2));
		imageRepository.save(ImageFixture.create(FilePurpose.GROUP_IMAGE, "test/image1", IMAGE_UUID_1));
		imageRepository.save(ImageFixture.create(FilePurpose.GROUP_IMAGE, "test/image2", IMAGE_UUID_2));
	}

	@Nested
	@DisplayName("하위그룹 생성")
	class CreateSubgroup {

		@Test
		@DisplayName("이미지 없이 하위그룹을 생성하면 memberCount가 1이고 creator가 자동 가입된다")
		void createSubgroup_withoutImage_success() {
			SubgroupCreateRequest request = SubgroupRequestFixture.createRequest("테스트하위그룹", "설명");

			SubgroupCreateResponse response = subgroupFacade.createSubgroup(group.getId(), member1.getId(), request);

			assertThat(response.data().id()).isNotNull();
			Subgroup subgroup = subgroupRepository.findById(response.data().id()).get();
			assertThat(subgroup.getMemberCount()).isEqualTo(1);
			assertThat(subgroupMemberRepository.findBySubgroupIdAndMember_IdAndDeletedAtIsNull(
				response.data().id(), member1.getId())).isPresent();
		}

		@Test
		@DisplayName("이미지 포함 생성 시 PENDING에서 ACTIVE로 전환된다")
		void createSubgroup_withImage_success() {
			SubgroupCreateRequest request = SubgroupRequestFixture.createRequestWithImage("이미지하위그룹",
				IMAGE_UUID_1.toString());

			SubgroupCreateResponse response = subgroupFacade.createSubgroup(group.getId(), member1.getId(), request);

			Image image = imageRepository.findByFileUuid(IMAGE_UUID_1).get();
			assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);
			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.SUBGROUP, response.data().id());
			assertThat(links).hasSize(1);
		}

		@Test
		@DisplayName("PASSWORD joinType로 생성 시 암호화된 password가 저장된다")
		void createSubgroup_withPasswordJoinType_success() {
			SubgroupCreateRequest request = SubgroupRequestFixture.createPasswordRequest("비밀번호하위그룹", "password123");

			SubgroupCreateResponse response = subgroupFacade.createSubgroup(group.getId(), member1.getId(), request);

			Subgroup subgroup = subgroupRepository.findById(response.data().id()).get();
			assertThat(subgroup.getJoinPassword()).isNotNull();
			assertThat(subgroup.getJoinPassword()).isNotEqualTo("password123");
		}

		@Test
		@DisplayName("동일 group 내 중복 이름은 예외를 발생시킨다")
		void createSubgroup_duplicateName_throwsBusinessException() {
			SubgroupCreateRequest request = SubgroupRequestFixture.createRequest("중복이름", "설명");
			subgroupFacade.createSubgroup(group.getId(), member1.getId(), request);

			assertThatThrownBy(() -> subgroupFacade.createSubgroup(group.getId(), member1.getId(), request))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("ACTIVE가 아닌 group에는 하위그룹을 생성할 수 없다")
		void createSubgroup_inactiveGroup_throwsBusinessException() {
			group.updateStatus(GroupStatus.INACTIVE);

			SubgroupCreateRequest request = SubgroupRequestFixture.createRequest("비활성그룹하위", "설명");

			assertThatThrownBy(() -> subgroupFacade.createSubgroup(group.getId(), member1.getId(), request))
				.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("하위그룹 가입")
	class JoinSubgroup {

		private Subgroup openSubgroup;
		private Subgroup passwordSubgroup;

		@BeforeEach
		void setUp() {
			openSubgroup = subgroupRepository.save(SubgroupFixture.create(
				group, "OPEN하위그룹", SubgroupJoinType.OPEN, 1));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(openSubgroup.getId(), member1));

			passwordSubgroup = subgroupRepository.save(SubgroupFixture.createPassword(
				group, "PASSWORD하위그룹", "$2a$10$encoded", 1));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(passwordSubgroup.getId(), member1));
		}

		@Test
		@DisplayName("OPEN joinType 하위그룹에 즉시 가입되고 memberCount가 증가한다")
		void joinSubgroup_openType_success() {
			subgroupFacade.joinSubgroup(group.getId(), openSubgroup.getId(), member2.getId(), null);

			Subgroup updated = subgroupRepository.findById(openSubgroup.getId()).get();
			assertThat(updated.getMemberCount()).isEqualTo(2);
		}

		@Test
		@DisplayName("PASSWORD joinType에서 올바른 password로 가입 성공")
		void joinSubgroup_withPassword_success() {
			SubgroupJoinRequest request = new SubgroupJoinRequest("correctPassword");

			assertThatThrownBy(() -> subgroupFacade.joinSubgroup(
				group.getId(), passwordSubgroup.getId(), member2.getId(), request))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("PASSWORD joinType에서 틀린 password는 예외 발생")
		void joinSubgroup_passwordMismatch_throwsBusinessException() {
			SubgroupJoinRequest request = new SubgroupJoinRequest("wrongPassword");

			assertThatThrownBy(() -> subgroupFacade.joinSubgroup(
				group.getId(), passwordSubgroup.getId(), member2.getId(), request))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("탈퇴했던 회원이 재가입 시 restore되고 memberCount가 재증가한다")
		void joinSubgroup_restore_success() {
			SubgroupMember membership = subgroupMemberRepository.save(
				SubgroupMember.create(openSubgroup.getId(), member2));
			openSubgroup.increaseMemberCount();
			membership.softDelete(java.time.Instant.now());
			openSubgroup.decreaseMemberCount();

			subgroupFacade.joinSubgroup(group.getId(), openSubgroup.getId(), member2.getId(), null);

			Subgroup updated = subgroupRepository.findById(openSubgroup.getId()).get();
			assertThat(updated.getMemberCount()).isEqualTo(2);
			SubgroupMember restored = subgroupMemberRepository.findBySubgroupIdAndMember_Id(
				openSubgroup.getId(), member2.getId()).get();
			assertThat(restored.getDeletedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("하위그룹 탈퇴")
	class WithdrawSubgroup {

		private Subgroup subgroup;

		@BeforeEach
		void setUp() {
			subgroup = subgroupRepository.save(SubgroupFixture.create(
				group, "탈퇴테스트하위그룹", SubgroupJoinType.OPEN, 2));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(subgroup.getId(), member1));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(subgroup.getId(), member2));
		}

		@Test
		@DisplayName("탈퇴 시 softDelete되고 memberCount가 감소한다")
		void withdrawSubgroup_success() {
			subgroupFacade.withdrawSubgroup(subgroup.getId(), member1.getId());

			Subgroup updated = subgroupRepository.findById(subgroup.getId()).get();
			assertThat(updated.getMemberCount()).isEqualTo(1);
			SubgroupMember membership = subgroupMemberRepository.findBySubgroupIdAndMember_Id(
				subgroup.getId(), member1.getId()).get();
			assertThat(membership.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("이미 탈퇴한 회원이 재탈퇴 시 idempotent하게 처리된다")
		void withdrawSubgroup_alreadyWithdrawn_idempotent() {
			subgroupFacade.withdrawSubgroup(subgroup.getId(), member1.getId());

			subgroupFacade.withdrawSubgroup(subgroup.getId(), member1.getId());

			Subgroup updated = subgroupRepository.findById(subgroup.getId()).get();
			assertThat(updated.getMemberCount()).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("하위그룹 수정")
	class UpdateSubgroup {

		private Subgroup subgroup;

		@BeforeEach
		void setUp() {
			subgroup = subgroupRepository.save(SubgroupFixture.createWithDescription(
				group, "수정전이름", "수정전설명", SubgroupJoinType.OPEN, 1));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(subgroup.getId(), member1));
		}

		@Test
		@DisplayName("이름을 변경하면 DB에 반영된다")
		void updateSubgroup_name_success() {
			SubgroupUpdateRequest request = new SubgroupUpdateRequest(
				objectMapper.valueToTree("수정후이름"), null, null);

			subgroupFacade.updateSubgroup(group.getId(), subgroup.getId(), member1.getId(), request);

			Subgroup updated = subgroupRepository.findById(subgroup.getId()).get();
			assertThat(updated.getName()).isEqualTo("수정후이름");
		}

		@Test
		@DisplayName("설명을 변경하면 DB에 반영된다")
		void updateSubgroup_description_success() {
			SubgroupUpdateRequest request = new SubgroupUpdateRequest(
				null, objectMapper.valueToTree("수정후설명"), null);

			subgroupFacade.updateSubgroup(group.getId(), subgroup.getId(), member1.getId(), request);

			Subgroup updated = subgroupRepository.findById(subgroup.getId()).get();
			assertThat(updated.getDescription()).isEqualTo("수정후설명");
		}

		@Test
		@DisplayName("profileImageFileUuid에 null 전달 시 이미지가 삭제된다")
		void updateSubgroup_removeImage_success() {
			domainImageRepository.save(DomainImage.create(
				DomainType.SUBGROUP, subgroup.getId(),
				imageRepository.findByFileUuid(IMAGE_UUID_1).get(), 0));

			SubgroupUpdateRequest request = new SubgroupUpdateRequest(
				null, null, objectMapper.nullNode());

			subgroupFacade.updateSubgroup(group.getId(), subgroup.getId(), member1.getId(), request);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.SUBGROUP, subgroup.getId());
			assertThat(links).isEmpty();
		}

		@Test
		@DisplayName("이미지를 교체하면 기존 이미지 삭제 후 새 이미지가 ACTIVE로 전환된다")
		void updateSubgroup_replaceImage_success() {
			SubgroupUpdateRequest request = new SubgroupUpdateRequest(
				null, null, objectMapper.valueToTree(IMAGE_UUID_2.toString()));

			subgroupFacade.updateSubgroup(group.getId(), subgroup.getId(), member1.getId(), request);

			Image newImage = imageRepository.findByFileUuid(IMAGE_UUID_2).get();
			assertThat(newImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);
			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.SUBGROUP, subgroup.getId());
			assertThat(links).hasSize(1);
		}
	}

	@Nested
	@DisplayName("하위그룹 목록 조회")
	class GetSubgroupList {

		@BeforeEach
		void setUp() {
			Subgroup subgroup1 = subgroupRepository.save(SubgroupFixture.create(
				group, "A하위그룹", SubgroupJoinType.OPEN, 1));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(subgroup1.getId(), member1));

			subgroupRepository.save(SubgroupFixture.create(
				group, "B하위그룹", SubgroupJoinType.OPEN, 2));
		}

		@Test
		@DisplayName("getMySubgroups는 회원이 속한 하위그룹만 반환한다")
		void getMySubgroups_returnsOnlyMemberSubgroups() {
			SubgroupListResponse response = subgroupFacade.getMySubgroups(
				group.getId(), member1.getId(), null, null, 10);

			assertThat(response.data()).hasSize(1);
			assertThat(response.data().get(0).getName()).isEqualTo("A하위그룹");
		}

		@Test
		@DisplayName("getGroupSubgroups는 ACTIVE 상태만 반환한다")
		void getGroupSubgroups_returnsActiveSubgroupsOnly() {
			CursorPageResponse<SubgroupListItem> response = subgroupFacade.getGroupSubgroups(
				group.getId(), member1.getId(), null, 10);

			assertThat(response.items()).hasSizeGreaterThanOrEqualTo(2);
		}

		@Test
		@DisplayName("searchGroupSubgroups는 keyword로 검색하고 memberCount desc 정렬된다")
		void searchGroupSubgroups_byKeyword_success() {
			CursorPageResponse<SubgroupListItem> response = subgroupFacade.searchGroupSubgroups(
				group.getId(), "B", null, 10);

			assertThat(response.items()).hasSize(1);
			assertThat(response.items().get(0).getName()).isEqualTo("B하위그룹");
		}

		@Test
		@DisplayName("getMySubgroups는 공격 문자열 키워드를 차단한다")
		void getMySubgroups_withUnsafeKeyword_throwsBusinessException() {
			assertThatThrownBy(() -> subgroupFacade.getMySubgroups(
				group.getId(), member1.getId(), "<script>alert('hacked')</script>", null, 10))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(SearchErrorCode.INVALID_SEARCH_KEYWORD.name());
		}

		@Test
		@DisplayName("searchGroupSubgroups는 공격 문자열 키워드를 차단한다")
		void searchGroupSubgroups_withUnsafeKeyword_throwsBusinessException() {
			assertThatThrownBy(() -> subgroupFacade.searchGroupSubgroups(
				group.getId(), "' or 1=1 --", null, 10))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(SearchErrorCode.INVALID_SEARCH_KEYWORD.name());
		}
	}

	@Nested
	@DisplayName("하위그룹 상세 조회")
	class GetSubgroupDetail {

		private Subgroup openSubgroup;
		private Subgroup passwordSubgroup;

		@BeforeEach
		void setUp() {
			openSubgroup = subgroupRepository.save(SubgroupFixture.create(
				group, "OPEN상세", SubgroupJoinType.OPEN, 1));

			passwordSubgroup = subgroupRepository.save(SubgroupFixture.createPassword(
				group, "PASSWORD상세", "$2a$10$encoded", 1));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(passwordSubgroup.getId(), member1));
		}

		@Test
		@DisplayName("OPEN joinType 하위그룹은 모든 회원이 조회 가능하다")
		void getSubgroup_openType_success() {
			SubgroupDetailResponse response = subgroupFacade.getSubgroup(openSubgroup.getId(), member2.getId());

			assertThat(response.data().subgroupId()).isEqualTo(openSubgroup.getId());
		}

		@Test
		@DisplayName("PASSWORD joinType 하위그룹은 비회원이 조회 불가하다")
		void getSubgroup_passwordType_nonMember_throwsBusinessException() {
			assertThatThrownBy(() -> subgroupFacade.getSubgroup(passwordSubgroup.getId(), member2.getId()))
				.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("하위그룹 회원 목록")
	class GetSubgroupMembers {

		private Subgroup subgroup;

		@BeforeEach
		void setUp() {
			subgroup = subgroupRepository.save(SubgroupFixture.create(
				group, "회원목록하위그룹", SubgroupJoinType.OPEN, 2));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(subgroup.getId(), member1));
			SubgroupMember member2Membership = subgroupMemberRepository.save(
				SubgroupMemberFixture.create(subgroup.getId(), member2));
			member2Membership.softDelete(java.time.Instant.now());
		}

		@Test
		@DisplayName("회원 목록을 반환한다")
		void getSubgroupMembers_returnsMemberList() {
			CursorPageResponse<SubgroupMemberListItem> response = subgroupFacade.getSubgroupMembers(
				subgroup.getId(), member1.getId(), null, 10);

			assertThat(response.items()).hasSizeGreaterThanOrEqualTo(1);
		}

		@Test
		@DisplayName("탈퇴한 회원은 목록에서 제외된다")
		void getSubgroupMembers_excludesWithdrawn() {
			CursorPageResponse<SubgroupMemberListItem> response = subgroupFacade.getSubgroupMembers(
				subgroup.getId(), member1.getId(), null, 10);

			assertThat(response.items()).hasSize(1);
			assertThat(response.items().get(0).memberId()).isEqualTo(member1.getId());
		}
	}
}
