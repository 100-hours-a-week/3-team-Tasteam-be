package com.tasteam.domain.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.auth.entity.RefreshToken;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("RefreshTokenRepository 테스트")
class RefreshTokenRepositoryTest {

	private static final Instant FUTURE_TIME = Instant.parse("2027-01-01T00:00:00Z");
	private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("토큰을 저장하면 기본 필드 매핑이 정상이다")
	void saveAndFind() {
		RefreshToken token = RefreshToken.issue(1L, "hash-abc-001", "family-001", FUTURE_TIME);

		RefreshToken saved = refreshTokenRepository.save(token);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getMemberId()).isEqualTo(1L);
		assertThat(saved.getTokenHash()).isEqualTo("hash-abc-001");
		assertThat(saved.getTokenFamilyId()).isEqualTo("family-001");
		assertThat(saved.getRotatedAt()).isNull();
		assertThat(saved.getRevokedAt()).isNull();
	}

	@Test
	@DisplayName("findByTokenHash - tokenHash로 조회 성공")
	void findByTokenHash_success() {
		refreshTokenRepository.save(
			RefreshToken.issue(1L, "hash-find-target", "family-001", FUTURE_TIME));
		entityManager.flush();
		entityManager.clear();

		var result = refreshTokenRepository.findByTokenHash("hash-find-target");

		assertThat(result).isPresent();
		assertThat(result.get().getTokenHash()).isEqualTo("hash-find-target");
	}

	@Test
	@DisplayName("save - 동일한 tokenHash를 저장하면 제약조건 예외가 발생한다")
	void save_duplicateTokenHash_throwsDataIntegrityViolationException() {
		refreshTokenRepository.saveAndFlush(
			RefreshToken.issue(1L, "duplicate-hash", "family-001", FUTURE_TIME));

		assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(
			RefreshToken.issue(2L, "duplicate-hash", "family-002", FUTURE_TIME)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("revokeByMemberId - 특정 회원의 토큰만 폐기되고 다른 회원 토큰은 영향받지 않는다")
	void revokeByMemberId_revokesOnlyTargetMember() {
		refreshTokenRepository.save(
			RefreshToken.issue(1L, "hash-member1-a", "family-a", FUTURE_TIME));
		refreshTokenRepository.save(
			RefreshToken.issue(1L, "hash-member1-b", "family-b", FUTURE_TIME));
		refreshTokenRepository.save(
			RefreshToken.issue(2L, "hash-member2-a", "family-c", FUTURE_TIME));
		entityManager.flush();
		entityManager.clear();

		int revokedCount = refreshTokenRepository.revokeByMemberId(1L, NOW);
		entityManager.flush();
		entityManager.clear();

		assertThat(revokedCount).isEqualTo(2);

		var member2Token = refreshTokenRepository.findByTokenHash("hash-member2-a");
		assertThat(member2Token).isPresent();
		assertThat(member2Token.get().getRevokedAt()).isNull();
	}

	@Test
	@DisplayName("revokeByTokenFamilyId - 해당 family의 모든 토큰에 revokedAt이 설정된다")
	void revokeByTokenFamilyId_revokesEntireFamily() {
		refreshTokenRepository.save(
			RefreshToken.issue(1L, "hash-fam-a1", "target-family", FUTURE_TIME));
		refreshTokenRepository.save(
			RefreshToken.issue(1L, "hash-fam-a2", "target-family", FUTURE_TIME));
		refreshTokenRepository.save(
			RefreshToken.issue(1L, "hash-fam-other", "other-family", FUTURE_TIME));
		entityManager.flush();
		entityManager.clear();

		int revokedCount = refreshTokenRepository.revokeByTokenFamilyId("target-family", NOW);
		entityManager.flush();
		entityManager.clear();

		assertThat(revokedCount).isEqualTo(2);

		var otherToken = refreshTokenRepository.findByTokenHash("hash-fam-other");
		assertThat(otherToken).isPresent();
		assertThat(otherToken.get().getRevokedAt()).isNull();
	}
}
