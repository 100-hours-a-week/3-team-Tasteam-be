package com.tasteam.fixture;

import java.time.Instant;

import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.subgroup.entity.SubgroupMember;

public final class SubgroupMemberFixture {
	private SubgroupMemberFixture() {}

	public static SubgroupMember create(Long subgroupId, Member member) {
		return SubgroupMember.create(subgroupId, member);
	}

	public static SubgroupMember createDeleted(Long subgroupId, Member member, Instant deletedAt) {
		SubgroupMember sm = SubgroupMember.create(subgroupId, member);
		sm.softDelete(deletedAt);
		return sm;
	}
}
