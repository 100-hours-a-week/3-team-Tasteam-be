package com.tasteam.domain.group.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.fixture.MemberFixture;

@UnitTest
@DisplayName("그룹 회원 엔티티")
class GroupMemberTest {

	@Nested
	@DisplayName("그룹 회원 생성·탈퇴·복귀")
	class GroupMemberLifecycle {

		@Test
		@DisplayName("그룹 회원을 생성하면 deletedAt이 null이다")
		void create_setsDeletedAtToNull() {
			GroupMember groupMember = GroupMember.create(1L, MemberFixture.create());

			assertThat(groupMember.getDeletedAt()).isNull();
			assertThat(groupMember.getGroupId()).isEqualTo(1L);
		}

		@Test
		@DisplayName("그룹 회원을 탈퇴 처리하면 deletedAt이 설정된다")
		void softDelete_setsDeletedAt() {
			GroupMember groupMember = GroupMember.create(1L, MemberFixture.create());
			Instant deletedAt = Instant.parse("2026-06-01T00:00:00Z");

			groupMember.softDelete(deletedAt);

			assertThat(groupMember.getDeletedAt()).isEqualTo(deletedAt);
		}

		@Test
		@DisplayName("탈퇴한 그룹 회원을 복귀 처리하면 deletedAt이 초기화된다")
		void restore_clearsDeletedAt() {
			GroupMember groupMember = GroupMember.create(1L, MemberFixture.create());
			groupMember.softDelete(Instant.parse("2026-06-01T00:00:00Z"));

			groupMember.restore();

			assertThat(groupMember.getDeletedAt()).isNull();
		}
	}
}
