package com.tasteam.domain.subgroup.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.domain.subgroup.type.SubgroupStatus;
import com.tasteam.fixture.GroupFixture;

@UnitTest
@DisplayName("하위그룹 엔티티")
class SubgroupTest {

	private Subgroup createSubgroup(Integer memberCount) {
		return Subgroup.builder()
			.group(GroupFixture.create())
			.name("테스트하위그룹")
			.joinType(SubgroupJoinType.OPEN)
			.status(SubgroupStatus.ACTIVE)
			.memberCount(memberCount)
			.build();
	}

	@Nested
	@DisplayName("하위그룹 회원 수 변경")
	class MemberCount {

		@Test
		@DisplayName("회원 수가 0일 때 증가하면 1이 된다")
		void increaseMemberCount_fromZero_incrementsToOne() {
			Subgroup subgroup = createSubgroup(0);

			subgroup.increaseMemberCount();

			assertThat(subgroup.getMemberCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("회원 수가 null일 때 증가하면 1이 된다")
		void increaseMemberCount_fromNull_incrementsToOne() {
			Subgroup subgroup = createSubgroup(null);

			subgroup.increaseMemberCount();

			assertThat(subgroup.getMemberCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("회원 수가 1일 때 감소하면 0이 된다")
		void decreaseMemberCount_fromOne_decreasesToZero() {
			Subgroup subgroup = createSubgroup(1);

			subgroup.decreaseMemberCount();

			assertThat(subgroup.getMemberCount()).isEqualTo(0);
		}

		@Test
		@DisplayName("회원 수가 0일 때 감소하면 0을 유지한다")
		void decreaseMemberCount_fromZero_staysAtZero() {
			Subgroup subgroup = createSubgroup(0);

			subgroup.decreaseMemberCount();

			assertThat(subgroup.getMemberCount()).isEqualTo(0);
		}

		@Test
		@DisplayName("회원 수가 null일 때 감소하면 0을 유지한다")
		void decreaseMemberCount_fromNull_staysAtZero() {
			Subgroup subgroup = createSubgroup(null);

			subgroup.decreaseMemberCount();

			assertThat(subgroup.getMemberCount()).isEqualTo(0);
		}
	}

	@Nested
	@DisplayName("하위그룹 삭제")
	class DeleteSubgroup {

		@Test
		@DisplayName("하위그룹을 삭제하면 deletedAt이 설정된다")
		void delete_setsDeletedAt() {
			Subgroup subgroup = createSubgroup(0);
			Instant deletedAt = Instant.parse("2026-06-01T00:00:00Z");

			subgroup.delete(deletedAt);

			assertThat(subgroup.getDeletedAt()).isEqualTo(deletedAt);
		}
	}
}
