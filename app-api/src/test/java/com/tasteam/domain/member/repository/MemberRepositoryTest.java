package com.tasteam.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.MemberRole;
import com.tasteam.domain.member.entity.MemberStatus;
import com.tasteam.fixture.MemberFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("MemberRepository 테스트")
class MemberRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("회원을 저장하면 기본 매핑이 정상이다")
	void saveAndFind() {
		Member member = MemberFixture.create();

		Member saved = memberRepository.save(member);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getEmail()).isEqualTo(MemberFixture.DEFAULT_EMAIL);
		assertThat(saved.getNickname()).isEqualTo(MemberFixture.DEFAULT_NICKNAME);
		assertThat(saved.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(saved.getRole()).isEqualTo(MemberRole.USER);
	}

	@Test
	@DisplayName("findByEmail - 저장된 회원의 이메일로 조회 성공")
	void findByEmail_success() {
		memberRepository.save(MemberFixture.create());
		entityManager.flush();
		entityManager.clear();

		var result = memberRepository.findByEmail(MemberFixture.DEFAULT_EMAIL);

		assertThat(result).isPresent();
		assertThat(result.get().getEmail()).isEqualTo(MemberFixture.DEFAULT_EMAIL);
	}

	@Test
	@DisplayName("findByIdAndDeletedAtIsNull - 탈퇴한 회원은 조회되지 않음")
	void findByIdAndDeletedAtIsNull_excludesWithdrawn() {
		Member member = memberRepository.save(MemberFixture.create());
		member.withdraw();
		entityManager.flush();
		entityManager.clear();

		var result = memberRepository.findByIdAndDeletedAtIsNull(member.getId());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("existsByEmail - 동일 이메일 회원이 존재하면 true")
	void existsByEmail_true() {
		memberRepository.save(MemberFixture.create());
		entityManager.flush();
		entityManager.clear();

		boolean exists = memberRepository.existsByEmail(MemberFixture.DEFAULT_EMAIL);

		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("existsByEmailAndIdNot - 다른 회원이 동일 이메일 사용 시 true")
	void existsByEmailAndIdNot_differentMember() {
		Member member1 = memberRepository.save(MemberFixture.create("shared@test.com", "회원1"));
		memberRepository.save(MemberFixture.create("other@test.com", "회원2"));
		entityManager.flush();
		entityManager.clear();

		boolean exists = memberRepository.existsByEmailAndIdNot("shared@test.com", member1.getId());

		assertThat(exists).isFalse();
	}
}
