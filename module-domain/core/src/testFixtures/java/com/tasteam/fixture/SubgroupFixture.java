package com.tasteam.fixture;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.domain.subgroup.type.SubgroupStatus;

public final class SubgroupFixture {
	private SubgroupFixture() {}

	public static Subgroup create(Group group, String name) {
		return create(group, name, SubgroupJoinType.OPEN, 0);
	}

	public static Subgroup create(Group group, String name, SubgroupJoinType joinType, int memberCount) {
		return Subgroup.builder()
			.group(group)
			.name(name)
			.joinType(joinType)
			.status(SubgroupStatus.ACTIVE)
			.memberCount(memberCount)
			.build();
	}

	public static Subgroup createWithDescription(Group group, String name, String description,
		SubgroupJoinType joinType, int memberCount) {
		return Subgroup.builder()
			.group(group)
			.name(name)
			.description(description)
			.joinType(joinType)
			.status(SubgroupStatus.ACTIVE)
			.memberCount(memberCount)
			.build();
	}

	public static Subgroup createPassword(Group group, String name, String encodedPassword, int memberCount) {
		return Subgroup.builder()
			.group(group)
			.name(name)
			.joinType(SubgroupJoinType.PASSWORD)
			.joinPassword(encodedPassword)
			.status(SubgroupStatus.ACTIVE)
			.memberCount(memberCount)
			.build();
	}
}
