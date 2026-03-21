package com.tasteam.fixture;

import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;

public final class MemberRequestFixture {

	public static final String DEFAULT_EMAIL = "new@example.com";
	public static final String DEFAULT_NICKNAME = "새닉네임";
	public static final String DEFAULT_INTRODUCTION = "안녕하세요";
	public static final String DEFAULT_PROFILE_IMAGE_UUID = "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012";

	private MemberRequestFixture() {}

	public static MemberProfileUpdateRequest profileUpdateRequest() {
		return profileUpdateRequest(DEFAULT_EMAIL, DEFAULT_NICKNAME, DEFAULT_INTRODUCTION, DEFAULT_PROFILE_IMAGE_UUID);
	}

	public static MemberProfileUpdateRequest profileUpdateRequest(String email, String nickname, String introduction,
		String profileImageFileUuid) {
		return new MemberProfileUpdateRequest(email, nickname, introduction, profileImageFileUuid);
	}
}
