package com.tasteam.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
import com.tasteam.fixture.MemberFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("MemberOAuthAccountRepository 테스트")
class MemberOAuthAccountRepositoryTest {

	@Autowired
	private MemberOAuthAccountRepository memberOAuthAccountRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("OAuth 계정을 저장하면 기본 매핑과 연관관계가 정상이다")
	void saveAndFind() {
		Member member = memberRepository.save(MemberFixture.create());
		MemberOAuthAccount oauthAccount = MemberOAuthAccount.create(
			"kakao", "kakao-user-123", "kakao@test.com", member);

		MemberOAuthAccount saved = memberOAuthAccountRepository.save(oauthAccount);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getProvider()).isEqualTo("kakao");
		assertThat(saved.getProviderUserId()).isEqualTo("kakao-user-123");
		assertThat(saved.getMember().getId()).isEqualTo(member.getId());
	}

	@Test
	@DisplayName("findByProviderAndProviderUserId - provider와 providerUserId 조합으로 조회 성공")
	void findByProviderAndProviderUserId_success() {
		Member member = memberRepository.save(MemberFixture.create());
		memberOAuthAccountRepository.save(
			MemberOAuthAccount.create("kakao", "kakao-user-123", "kakao@test.com", member));
		entityManager.flush();
		entityManager.clear();

		var result = memberOAuthAccountRepository.findByProviderAndProviderUserId("kakao", "kakao-user-123");

		assertThat(result).isPresent();
		assertThat(result.get().getProvider()).isEqualTo("kakao");
		assertThat(result.get().getProviderUserId()).isEqualTo("kakao-user-123");
	}

	@Test
	@DisplayName("save - 동일한 provider와 providerUserId를 저장하면 제약조건 예외가 발생한다")
	void save_duplicateProviderAndProviderUserId_throwsDataIntegrityViolationException() {
		Member member1 = memberRepository.save(MemberFixture.create("user1@test.com", "회원1"));
		Member member2 = memberRepository.save(MemberFixture.create("user2@test.com", "회원2"));
		memberOAuthAccountRepository.save(
			MemberOAuthAccount.create("kakao", "same-provider-id", "kakao@test.com", member1));
		memberOAuthAccountRepository.save(
			MemberOAuthAccount.create("kakao", "same-provider-id", "kakao2@test.com", member2));

		assertThatThrownBy(() -> entityManager.flush())
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
