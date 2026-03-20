package com.tasteam.fixture;

import java.time.Instant;

import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.member.entity.Member;

public final class GroupMemberFixture {
	private GroupMemberFixture() {}

	public static GroupMember create(Long groupId, Member member) {
		return GroupMember.create(groupId, member);
	}

	public static GroupMember createDeleted(Long groupId, Member member, Instant deletedAt) {
		GroupMember gm = GroupMember.create(groupId, member);
		gm.softDelete(deletedAt);
		return gm;
	}
}
