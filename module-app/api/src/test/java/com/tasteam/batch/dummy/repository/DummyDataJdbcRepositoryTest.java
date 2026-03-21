package com.tasteam.batch.dummy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
import com.tasteam.domain.member.repository.MemberOAuthAccountRepository;
import com.tasteam.domain.member.repository.MemberRepository;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("[유닛](Dummy) DummyDataJdbcRepository 단위 테스트")
class DummyDataJdbcRepositoryTest {

	@Autowired
	private DummyDataJdbcRepository dummyDataJdbcRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberOAuthAccountRepository memberOAuthAccountRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("더미 멤버를 저장하면 부하테스트 TEST OAuth 계정도 함께 생성된다")
	void insertMembersWithTestOAuthAccounts_createsOAuthAccounts() {
		// given
		List<String> emails = List.of("dummy-a@dummy.tasteam.kr", "dummy-b@dummy.tasteam.kr");
		List<String> nicknames = List.of("더미회원1", "더미회원2");
		List<String> identifiers = List.of("test-user-001", "test-user-002");

		// when
		List<Long> memberIds = dummyDataJdbcRepository.insertMembersWithTestOAuthAccounts(emails, nicknames,
			identifiers);
		entityManager.flush();
		entityManager.clear();

		// then
		assertThat(memberIds).hasSize(2);
		MemberOAuthAccount firstAccount = memberOAuthAccountRepository
			.findByProviderAndProviderUserId("TEST", "test-user-001")
			.orElseThrow();
		assertThat(firstAccount.getMember().getId()).isEqualTo(memberIds.get(0));
		assertThat(firstAccount.getMember().getEmail()).isEqualTo("dummy-a@dummy.tasteam.kr");
		assertThat(firstAccount.getProviderUserEmail()).isEqualTo("test-user-001@test.local");
	}

	@Test
	@DisplayName("같은 부하테스트 식별자로 다시 시딩하면 TEST OAuth 계정이 최신 더미 멤버를 가리킨다")
	void insertMembersWithTestOAuthAccounts_updatesLatestMemberOnIdentifierConflict() {
		// given
		List<Long> firstIds = dummyDataJdbcRepository.insertMembersWithTestOAuthAccounts(
			List.of("dummy-first@dummy.tasteam.kr"),
			List.of("첫더미회원"),
			List.of("test-user-001"));

		// when
		List<Long> secondIds = dummyDataJdbcRepository.insertMembersWithTestOAuthAccounts(
			List.of("dummy-second@dummy.tasteam.kr"),
			List.of("두번째더미회원"),
			List.of("test-user-001"));
		entityManager.flush();
		entityManager.clear();

		// then
		MemberOAuthAccount account = memberOAuthAccountRepository
			.findByProviderAndProviderUserId("TEST", "test-user-001")
			.orElseThrow();
		assertThat(account.getMember().getId()).isEqualTo(secondIds.get(0));
		assertThat(memberRepository.findById(firstIds.get(0))).isPresent();
		assertThat(memberRepository.findById(secondIds.get(0))).isPresent();
	}

	@Test
	@DisplayName("더미 데이터를 삭제하면 TEST OAuth 계정도 함께 정리된다")
	void deleteDummyData_removesOAuthAccounts() {
		// given
		List<Long> memberIds = dummyDataJdbcRepository.insertMembersWithTestOAuthAccounts(
			List.of("dummy-cleanup@dummy.tasteam.kr"),
			List.of("정리대상회원"),
			List.of("test-user-010"));

		// when
		dummyDataJdbcRepository.deleteDummyData();
		entityManager.flush();
		entityManager.clear();

		// then
		assertThat(memberRepository.findById(memberIds.get(0))).isEmpty();
		assertThat(memberOAuthAccountRepository.findByProviderAndProviderUserId("TEST", "test-user-010")).isEmpty();
	}

	@Test
	@DisplayName("기존 고정 ID 데이터가 있어도 member/member_oauth_account 시퀀스를 보정해 저장한다")
	void insertMembersWithTestOAuthAccounts_syncsSequencesToExistingMaxIds() {
		// given
		entityManager.createNativeQuery("""
			INSERT INTO member (id, email, nickname, status, role, created_at, updated_at)
			VALUES (1001, 'seed-member@tasteam.dev', '시드회원', 'ACTIVE', 'USER', now(), now())
			""").executeUpdate();
		entityManager
			.createNativeQuery(
				"""
					INSERT INTO member_oauth_account (id, member_id, provider, provider_user_id, provider_user_email, created_at)
					VALUES (9001, 1001, 'TEST', 'seed-user', 'seed-user@test.local', now())
					""")
			.executeUpdate();
		entityManager.createNativeQuery("SELECT setval('member_seq', 1001, false)").getSingleResult();
		entityManager.createNativeQuery("SELECT setval('member_oauth_account_seq', 9001, false)").getSingleResult();
		entityManager.flush();
		entityManager.clear();

		// when
		List<Long> memberIds = dummyDataJdbcRepository.insertMembersWithTestOAuthAccounts(
			List.of("dummy-seq@dummy.tasteam.kr"),
			List.of("시퀀스보정회원"),
			List.of("test-user-099"));
		entityManager.flush();
		entityManager.clear();

		// then
		assertThat(memberIds).hasSize(1);
		assertThat(memberIds.get(0)).isGreaterThan(1001L);
		MemberOAuthAccount account = memberOAuthAccountRepository
			.findByProviderAndProviderUserId("TEST", "test-user-099")
			.orElseThrow();
		assertThat(account.getId()).isGreaterThan(9001L);
		assertThat(account.getMember().getId()).isEqualTo(memberIds.get(0));
	}

	@Test
	@DisplayName("기존 고정 ID 데이터가 있어도 identity 시퀀스를 보정해 음식점을 저장한다")
	void insertRestaurants_syncsIdentitySequenceToExistingMaxId() {
		// given
		entityManager.createNativeQuery("""
			INSERT INTO restaurant (id, name, full_address, location, vector_epoch, created_at, updated_at)
			VALUES (6001, '시드식당', '서울특별시 마포구', ST_SetSRID(ST_MakePoint(126.9, 37.5), 4326), 0, now(), now())
			""").executeUpdate();
		entityManager.createNativeQuery(
			"SELECT setval(pg_get_serial_sequence('restaurant', 'id'), 6001, false)").getSingleResult();
		entityManager.flush();
		entityManager.clear();

		// when
		List<Long> restaurantIds = dummyDataJdbcRepository.insertRestaurants(
			List.of("더미식당"),
			List.of("서울특별시 강남구"));

		// then
		assertThat(restaurantIds).hasSize(1);
		assertThat(restaurantIds.get(0)).isGreaterThan(6001L);
	}

	@Test
	@DisplayName("기존 고정 ID 데이터가 있어도 group_member/subgroup_member 시퀀스를 보정해 저장한다")
	void insertGroupAndSubgroupMembers_syncsSequencesToExistingMaxIds() {
		// given
		entityManager.createNativeQuery("""
			INSERT INTO member (id, email, nickname, status, role, created_at, updated_at)
			VALUES
			  (1001, 'seed-a@tasteam.dev', '시드회원A', 'ACTIVE', 'USER', now(), now()),
			  (1002, 'seed-b@tasteam.dev', '시드회원B', 'ACTIVE', 'USER', now(), now())
			""").executeUpdate();
		entityManager.createNativeQuery("""
			INSERT INTO "group" (id, name, type, address, location, join_type, status, created_at, updated_at)
			VALUES (2001, '시드그룹', 'UNOFFICIAL', '서울특별시 마포구', ST_SetSRID(ST_MakePoint(126.9, 37.5), 4326),
			        'PASSWORD', 'ACTIVE', now(), now())
			""").executeUpdate();
		entityManager.createNativeQuery("""
			INSERT INTO subgroup (id, group_id, name, join_type, status, member_count, created_at, updated_at)
			VALUES (4001, 2001, '시드서브그룹', 'OPEN', 'ACTIVE', 1, now(), now())
			""").executeUpdate();
		entityManager.createNativeQuery("""
			INSERT INTO group_member (id, group_id, member_id, created_at)
			VALUES (3001, 2001, 1001, now())
			""").executeUpdate();
		entityManager.createNativeQuery("""
			INSERT INTO subgroup_member (id, subgroup_id, member_id, created_at)
			VALUES (5001, 4001, 1001, now())
			""").executeUpdate();
		entityManager.createNativeQuery("SELECT setval('group_member_seq', 3001, false)").getSingleResult();
		entityManager.createNativeQuery("SELECT setval('subgroup_member_seq', 5001, false)").getSingleResult();
		entityManager.flush();
		entityManager.clear();

		// when
		dummyDataJdbcRepository.insertGroupMembers(List.of(new long[] {2001L, 1002L}));
		dummyDataJdbcRepository.insertSubgroupMembers(List.of(new long[] {4001L, 1002L}));
		entityManager.flush();
		entityManager.clear();

		// then
		Number groupMemberCount = (Number)entityManager.createNativeQuery(
			"SELECT COUNT(*) FROM group_member WHERE group_id = 2001 AND member_id = 1002").getSingleResult();
		Number subgroupMemberCount = (Number)entityManager.createNativeQuery(
			"SELECT COUNT(*) FROM subgroup_member WHERE subgroup_id = 4001 AND member_id = 1002").getSingleResult();
		assertThat(groupMemberCount.longValue()).isEqualTo(1L);
		assertThat(subgroupMemberCount.longValue()).isEqualTo(1L);
	}
}
