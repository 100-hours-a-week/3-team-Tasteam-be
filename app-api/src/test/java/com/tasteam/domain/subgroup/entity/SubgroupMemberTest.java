package com.tasteam.domain.subgroup.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.fixture.MemberFixture;

@UnitTest
@DisplayName("하위그룹 회원 엔티티")
class SubgroupMemberTest {

	@Nested
	@DisplayName("하위그룹 회원 생성·탈퇴·복귀")
	class SubgroupMemberLifecycle {

		@Test
		@DisplayName("하위그룹 회원을 생성하면 deletedAt이 null이다")
		void create_setsDeletedAtToNull() {
			SubgroupMember subgroupMember = SubgroupMember.create(1L, MemberFixture.create());

			assertThat(subgroupMember.getDeletedAt()).isNull();
			assertThat(subgroupMember.getSubgroupId()).isEqualTo(1L);
		}

		@Test
		@DisplayName("하위그룹 회원을 탈퇴 처리하면 deletedAt이 설정된다")
		void softDelete_setsDeletedAt() {
			SubgroupMember subgroupMember = SubgroupMember.create(1L, MemberFixture.create());
			Instant deletedAt = Instant.parse("2026-06-01T00:00:00Z");

			subgroupMember.softDelete(deletedAt);

			assertThat(subgroupMember.getDeletedAt()).isEqualTo(deletedAt);
		}

		@Test
		@DisplayName("탈퇴한 하위그룹 회원을 복귀 처리하면 deletedAt이 초기화된다")
		void restore_clearsDeletedAt() {
			SubgroupMember subgroupMember = SubgroupMember.create(1L, MemberFixture.create());
			subgroupMember.softDelete(Instant.parse("2026-06-01T00:00:00Z"));

			subgroupMember.restore();

			assertThat(subgroupMember.getDeletedAt()).isNull();
		}
	}
}
