package com.tasteam.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.config.fake.FakeEmailSender;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.dto.GroupMemberListResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupUpdateRequest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupAuthCode;
import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.group.repository.GroupAuthCodeRepository;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.entity.SubgroupMember;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.GroupMemberFixture;
import com.tasteam.fixture.GroupRequestFixture;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.fixture.SubgroupFixture;
import com.tasteam.fixture.SubgroupMemberFixture;
import com.tasteam.global.exception.business.BusinessException;

@ServiceIntegrationTest
@Transactional
@DisplayName("GroupFacade 통합 테스트")
class GroupFacadeIntegrationTest {

	private static final UUID LOGO_UUID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID LOGO_UUID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private GroupFacade groupFacade;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private GroupAuthCodeRepository groupAuthCodeRepository;

	@Autowired
	private GroupInviteTokenService groupInviteTokenService;

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

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private FakeEmailSender emailSender;

	private Member member1;
	private Member member2;
	private Member member3;

	@BeforeEach
	void setUp() {
		redisTemplate.execute((RedisCallback<Void>)connection -> {
			connection.serverCommands().flushAll();
			return null;
		});
		emailSender.clear();

		member1 = memberRepository.save(MemberFixture.create("member1@test.com", "회원1"));
		member2 = memberRepository.save(MemberFixture.create("member2@test.com", "회원2"));
		member3 = memberRepository.save(MemberFixture.create("member3@example.com", "회원3"));
		imageRepository.save(ImageFixture.create(FilePurpose.GROUP_IMAGE, "test/logo1", LOGO_UUID_1));
		imageRepository.save(ImageFixture.create(FilePurpose.GROUP_IMAGE, "test/logo2", LOGO_UUID_2));
	}

	@Nested
	@DisplayName("그룹 생성")
	class CreateGroup {

		@Test
		@DisplayName("EMAIL joinType으로 그룹을 생성하면 emailDomain이 저장된다")
		void createGroup_emailJoinType_success() {
			GroupCreateRequest request = GroupRequestFixture.createEmailGroupRequest("이메일그룹", "example.com");

			GroupCreateResponse response = groupFacade.createGroup(request);

			Group group = groupRepository.findById(response.id()).get();
			assertThat(group.getEmailDomain()).isEqualTo("example.com");
		}

		@Test
		@DisplayName("EMAIL joinType + 로고 이미지 포함 생성 시 PENDING에서 ACTIVE로 전환된다")
		void createGroup_emailJoinType_withLogo_success() {
			GroupCreateRequest request = GroupRequestFixture.createEmailGroupRequestWithLogo(
				"로고그룹", LOGO_UUID_1.toString(), "example.com");

			GroupCreateResponse response = groupFacade.createGroup(request);

			Image logo = imageRepository.findByFileUuid(LOGO_UUID_1).get();
			assertThat(logo.getStatus()).isEqualTo(ImageStatus.ACTIVE);
			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.GROUP, response.id());
			assertThat(links).hasSize(1);
		}

		@Test
		@DisplayName("PASSWORD joinType으로 생성 시 GroupAuthCode에 암호화된 code가 저장된다")
		void createGroup_passwordJoinType_success() {
			GroupCreateRequest request = GroupRequestFixture.createPasswordGroupRequest(
				"비밀번호그룹", "password123");

			GroupCreateResponse response = groupFacade.createGroup(request);

			GroupAuthCode authCode = groupAuthCodeRepository.findByGroupId(response.id()).get();
			assertThat(authCode.getCode()).isNotEqualTo("password123");
			assertThat(authCode.getExpiresAt()).isNull();
		}

		@Test
		@DisplayName("중복된 그룹 이름은 예외를 발생시킨다")
		void createGroup_duplicateName_throwsBusinessException() {
			GroupCreateRequest request = GroupRequestFixture.createEmailGroupRequest("중복그룹", "example.com");
			groupFacade.createGroup(request);

			assertThatThrownBy(() -> groupFacade.createGroup(request))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("EMAIL joinType인데 emailDomain이 null이면 예외를 발생시킨다")
		void createGroup_invalidEmailDomain_throwsBusinessException() {
			GroupCreateRequest request = GroupRequestFixture.createEmailGroupRequest("도메인없음그룹", null);

			assertThatThrownBy(() -> groupFacade.createGroup(request))
				.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("그룹 조회")
	class GetGroup {

		private Group group;

		@BeforeEach
		void setUp() {
			group = groupRepository.save(GroupFixture.create("조회그룹", "서울시 강남구"));
			groupMemberRepository.save(GroupMemberFixture.create(group.getId(), member1));
			groupMemberRepository.save(GroupMemberFixture.create(group.getId(), member2));
		}

		@Test
		@DisplayName("groupId로 그룹을 조회하면 memberCount가 포함된다")
		void getGroup_success() {
			GroupGetResponse response = groupFacade.getGroup(group.getId());

			assertThat(response.data().groupId()).isEqualTo(group.getId());
			assertThat(response.data().memberCount()).isEqualTo(2);
		}

		@Test
		@DisplayName("존재하지 않는 그룹은 예외를 발생시킨다")
		void getGroup_notFound_throwsBusinessException() {
			assertThatThrownBy(() -> groupFacade.getGroup(999999L))
				.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("그룹 수정")
	class UpdateGroup {

		private Group group;

		@BeforeEach
		void setUp() {
			group = groupRepository.save(GroupFixture.create("수정전그룹", "서울시 강남구"));
		}

		@Test
		@DisplayName("그룹 이름을 변경하면 DB에 반영된다")
		void updateGroup_name_success() {
			GroupUpdateRequest request = new GroupUpdateRequest(
				objectMapper.valueToTree("수정후그룹"), null, null, null, null, null);

			groupFacade.updateGroup(group.getId(), request);

			Group updated = groupRepository.findById(group.getId()).get();
			assertThat(updated.getName()).isEqualTo("수정후그룹");
		}

		@Test
		@DisplayName("그룹 status를 변경하면 DB에 반영된다")
		void updateGroup_status_success() {
			GroupUpdateRequest request = new GroupUpdateRequest(
				null, null, null, null, objectMapper.valueToTree("INACTIVE"), null);

			groupFacade.updateGroup(group.getId(), request);

			Group updated = groupRepository.findById(group.getId()).get();
			assertThat(updated.getStatus()).isEqualTo(GroupStatus.INACTIVE);
		}

		@Test
		@DisplayName("logoImageFileUuid에 null 전달 시 로고가 삭제된다")
		void updateGroup_removeLogo_success() {
			domainImageRepository.save(DomainImage.create(
				DomainType.GROUP, group.getId(),
				imageRepository.findByFileUuid(LOGO_UUID_1).get(), 0));

			GroupUpdateRequest request = new GroupUpdateRequest(
				null, null, null, null, null, objectMapper.nullNode());

			groupFacade.updateGroup(group.getId(), request);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.GROUP, group.getId());
			assertThat(links).isEmpty();
		}

		@Test
		@DisplayName("로고를 교체하면 기존 로고 삭제 후 새 로고가 ACTIVE로 전환된다")
		void updateGroup_replaceLogo_success() {
			GroupUpdateRequest request = new GroupUpdateRequest(
				null, null, null, null, null, objectMapper.valueToTree(LOGO_UUID_2.toString()));

			groupFacade.updateGroup(group.getId(), request);

			Image newLogo = imageRepository.findByFileUuid(LOGO_UUID_2).get();
			assertThat(newLogo.getStatus()).isEqualTo(ImageStatus.ACTIVE);
			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainId(
				DomainType.GROUP, group.getId());
			assertThat(links).hasSize(1);
		}
	}

	@Nested
	@DisplayName("이메일 인증 발송")
	class SendEmailVerification {

		private Group emailGroup;

		@BeforeEach
		void setUp() {
			GroupCreateRequest request = GroupRequestFixture.createEmailGroupRequest("이메일인증그룹", "example.com");
			GroupCreateResponse response = groupFacade.createGroup(request);
			emailGroup = groupRepository.findById(response.id()).get();
		}

		@Test
		@DisplayName("인증 이메일이 입력한 주소로 발송된다")
		void sendGroupEmailVerification_emailSentToRecipient() {
			// when
			groupFacade.sendGroupEmailVerification(
				emailGroup.getId(),
				member3.getId(),
				"127.0.0.1",
				"user@example.com");

			// then
			assertThat(emailSender.hasEmailSentTo("user@example.com")).isTrue();
		}

		@Test
		@DisplayName("인증 발송 응답에 만료시간이 포함된다")
		void sendGroupEmailVerification_expiresAtIsReturned() {
			// when
			var response = groupFacade.sendGroupEmailVerification(
				emailGroup.getId(),
				member3.getId(),
				"127.0.0.1",
				"user@example.com");

			// then
			assertThat(response.expiresAt()).isNotNull().isAfter(Instant.now());
		}

		@Test
		@DisplayName("이메일 도메인이 일치하지 않으면 인증 발송에 실패한다")
		void sendGroupEmailVerification_emailDomainMismatch_throwsBusinessException() {
			// when & then
			assertThatThrownBy(() -> groupFacade.sendGroupEmailVerification(
				emailGroup.getId(),
				member3.getId(),
				"127.0.0.1",
				"user@other.com"))
				.isInstanceOf(BusinessException.class);
		}

	}

	@Nested
	@DisplayName("이메일 인증 가입")
	class AuthenticateByEmail {

		private Group emailGroup;
		private String verificationToken;

		@BeforeEach
		void setUp() {
			GroupCreateRequest request = GroupRequestFixture.createEmailGroupRequest("이메일가입그룹", "example.com");
			GroupCreateResponse response = groupFacade.createGroup(request);
			emailGroup = groupRepository.findById(response.id()).get();
			groupFacade.sendGroupEmailVerification(emailGroup.getId(), member3.getId(), "127.0.0.1",
				"user@example.com");
			verificationToken = groupInviteTokenService.issue(emailGroup.getId(), member3.getEmail()).token();
		}

		@Test
		@DisplayName("올바른 토큰으로 인증 시 가입 성공하고 GroupMember가 생성된다")
		void authenticateGroupByEmail_success() {
			GroupEmailAuthenticationResponse response = groupFacade.authenticateGroupByEmail(
				emailGroup.getId(), member3.getId(), verificationToken);

			assertThat(response.verified()).isTrue();
			assertThat(groupMemberRepository.findByGroupIdAndMember_IdAndDeletedAtIsNull(
				emailGroup.getId(), member3.getId())).isPresent();
		}

		@Test
		@DisplayName("토큰의 이메일과 로그인 사용자 이메일이 다르면 예외를 발생시킨다")
		void authenticateGroupByEmail_emailMismatch_throwsBusinessException() {
			assertThatThrownBy(() -> groupFacade.authenticateGroupByEmail(
				emailGroup.getId(), member1.getId(), verificationToken))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("유효하지 않은 토큰은 예외를 발생시킨다")
		void authenticateGroupByEmail_invalidToken_throwsBusinessException() {
			assertThatThrownBy(() -> groupFacade.authenticateGroupByEmail(
				emailGroup.getId(), member3.getId(), "invalid-token"))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("이전 탈퇴 회원이 재가입 시 restore된다")
		void authenticateGroupByEmail_restore_success() {
			GroupMember membership = groupMemberRepository.save(GroupMemberFixture.create(emailGroup.getId(), member3));
			membership.softDelete(Instant.now());

			groupFacade.authenticateGroupByEmail(emailGroup.getId(), member3.getId(), verificationToken);

			GroupMember restored = groupMemberRepository.findByGroupIdAndMember_Id(
				emailGroup.getId(), member3.getId()).get();
			assertThat(restored.getDeletedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("비밀번호 인증 가입")
	class AuthenticateByPassword {

		private Group passwordGroup;

		@BeforeEach
		void setUp() {
			GroupCreateRequest request = GroupRequestFixture.createPasswordGroupRequest(
				"비밀번호가입그룹", "correctPassword");
			GroupCreateResponse response = groupFacade.createGroup(request);
			passwordGroup = groupRepository.findById(response.id()).get();
		}

		@Test
		@DisplayName("올바른 password로 가입 성공한다")
		void authenticateGroupByPassword_success() {
			GroupPasswordAuthenticationResponse response = groupFacade.authenticateGroupByPassword(
				passwordGroup.getId(), member2.getId(), "correctPassword");

			assertThat(response.verified()).isTrue();
			assertThat(groupMemberRepository.findByGroupIdAndMember_IdAndDeletedAtIsNull(
				passwordGroup.getId(), member2.getId())).isPresent();
		}

		@Test
		@DisplayName("틀린 password는 예외를 발생시킨다")
		void authenticateGroupByPassword_wrongPassword_throwsBusinessException() {
			assertThatThrownBy(() -> groupFacade.authenticateGroupByPassword(
				passwordGroup.getId(), member2.getId(), "wrongPassword"))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("이전 탈퇴 회원이 재가입 시 restore된다")
		void authenticateGroupByPassword_restore_success() {
			GroupMember membership = groupMemberRepository
				.save(GroupMemberFixture.create(passwordGroup.getId(), member2));
			membership.softDelete(Instant.now());

			groupFacade.authenticateGroupByPassword(passwordGroup.getId(), member2.getId(), "correctPassword");

			GroupMember restored = groupMemberRepository.findByGroupIdAndMember_Id(
				passwordGroup.getId(), member2.getId()).get();
			assertThat(restored.getDeletedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("그룹 탈퇴")
	class WithdrawGroup {

		private Group group;
		private Subgroup subgroup1;
		private Subgroup subgroup2;

		@BeforeEach
		void setUp() {
			group = groupRepository.save(GroupFixture.create("탈퇴테스트그룹", "서울시 강남구"));
			groupMemberRepository.save(GroupMemberFixture.create(group.getId(), member1));

			subgroup1 = subgroupRepository.save(SubgroupFixture.create(
				group, "하위그룹1", SubgroupJoinType.OPEN, 1));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(subgroup1.getId(), member1));

			subgroup2 = subgroupRepository.save(SubgroupFixture.create(
				group, "하위그룹2", SubgroupJoinType.OPEN, 1));
			subgroupMemberRepository.save(SubgroupMemberFixture.create(subgroup2.getId(), member1));
		}

		@Test
		@DisplayName("그룹 탈퇴 시 GroupMember가 softDelete된다")
		void withdrawGroup_success() {
			groupFacade.withdrawGroup(group.getId(), member1.getId());

			GroupMember membership = groupMemberRepository.findByGroupIdAndMember_Id(
				group.getId(), member1.getId()).get();
			assertThat(membership.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("이미 탈퇴한 회원이 재탈퇴 시 idempotent하게 처리된다")
		void withdrawGroup_alreadyWithdrawn_idempotent() {
			groupFacade.withdrawGroup(group.getId(), member1.getId());

			groupFacade.withdrawGroup(group.getId(), member1.getId());

			GroupMember membership = groupMemberRepository.findByGroupIdAndMember_Id(
				group.getId(), member1.getId()).get();
			assertThat(membership.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("그룹 탈퇴 시 모든 하위그룹에서도 cascading soft delete된다")
		void withdrawGroup_cascadesToSubgroups() {
			groupFacade.withdrawGroup(group.getId(), member1.getId());

			SubgroupMember sub1Membership = subgroupMemberRepository.findBySubgroupIdAndMember_Id(
				subgroup1.getId(), member1.getId()).get();
			SubgroupMember sub2Membership = subgroupMemberRepository.findBySubgroupIdAndMember_Id(
				subgroup2.getId(), member1.getId()).get();
			assertThat(sub1Membership.getDeletedAt()).isNotNull();
			assertThat(sub2Membership.getDeletedAt()).isNotNull();
		}
	}

	@Nested
	@DisplayName("그룹 회원 목록")
	class GetGroupMembers {

		private Group group;

		@BeforeEach
		void setUp() {
			group = groupRepository.save(GroupFixture.create("회원목록그룹", "서울시 강남구"));
			groupMemberRepository.save(GroupMemberFixture.create(group.getId(), member1));
			GroupMember member2Membership = groupMemberRepository.save(GroupMemberFixture.createDeleted(
				group.getId(), member2, Instant.now()));
		}

		@Test
		@DisplayName("그룹 회원 목록을 반환한다")
		void getGroupMembers_returnsMemberList() {
			GroupMemberListResponse response = groupFacade.getGroupMembers(group.getId(), null, 10);

			assertThat(response.data()).hasSizeGreaterThanOrEqualTo(1);
		}

		@Test
		@DisplayName("탈퇴한 회원은 목록에서 제외된다")
		void getGroupMembers_excludesWithdrawn() {
			GroupMemberListResponse response = groupFacade.getGroupMembers(group.getId(), null, 10);

			assertThat(response.data()).hasSize(1);
			assertThat(response.data().get(0).memberId()).isEqualTo(member1.getId());
		}
	}
}
