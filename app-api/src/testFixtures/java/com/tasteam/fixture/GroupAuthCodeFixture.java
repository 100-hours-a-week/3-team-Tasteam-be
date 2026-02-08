package com.tasteam.fixture;

import java.time.Instant;

import com.tasteam.domain.group.entity.GroupAuthCode;

public final class GroupAuthCodeFixture {
	private GroupAuthCodeFixture() {}

	public static GroupAuthCode create(Long groupId, String code, Instant expiresAt) {
		return GroupAuthCode.create(groupId, code, expiresAt);
	}

	public static GroupAuthCode expire(GroupAuthCode authCode, Instant expiresAt) {
		authCode.expire(expiresAt);
		return authCode;
	}
}
