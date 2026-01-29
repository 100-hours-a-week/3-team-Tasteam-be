package com.tasteam.fixture;

import java.lang.reflect.Field;

import com.tasteam.domain.member.entity.Member;

public final class MemberFixture {

	public static final String DEFAULT_EMAIL = "test@example.com";
	public static final String DEFAULT_NICKNAME = "테스트유저";
	public static final String DEFAULT_PROFILE_IMAGE_URL = "https://example.com/profile.jpg";

	public static final String UPDATED_NICKNAME = "변경된닉네임";
	public static final String UPDATED_EMAIL = "updated@example.com";

	private MemberFixture() {}

	public static Member create() {
		return Member.create(DEFAULT_EMAIL, DEFAULT_NICKNAME);
	}

	public static Member create(String email, String nickname) {
		return Member.create(email, nickname);
	}

	public static Member createWithId(Long id) {
		Member member = create();
		setId(member, id);
		return member;
	}

	public static Member createWithId(Long id, String email, String nickname) {
		Member member = create(email, nickname);
		setId(member, id);
		return member;
	}

	public static Member createWithdrawn() {
		Member member = create();
		member.withdraw();
		return member;
	}

	public static Member createBlocked() {
		Member member = create();
		member.block();
		return member;
	}

	private static void setId(Member member, Long id) {
		try {
			Field idField = Member.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(member, id);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("Failed to set member id", e);
		}
	}
}
