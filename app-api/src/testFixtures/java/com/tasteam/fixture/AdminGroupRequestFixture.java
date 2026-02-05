package com.tasteam.fixture;

import com.tasteam.domain.admin.dto.request.AdminGroupCreateRequest;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupType;

public final class AdminGroupRequestFixture {

	public static final String DEFAULT_NAME = "관리자 테스트 그룹";
	public static final String DEFAULT_ADDRESS = "서울특별시 강남구";

	private AdminGroupRequestFixture() {}

	public static AdminGroupCreateRequest createRequest() {
		return new AdminGroupCreateRequest(
			DEFAULT_NAME, null, GroupType.UNOFFICIAL, DEFAULT_ADDRESS, null, GroupJoinType.PASSWORD, null);
	}

	public static AdminGroupCreateRequest createRequest(String logoImageFileUuid) {
		return new AdminGroupCreateRequest(
			DEFAULT_NAME, logoImageFileUuid, GroupType.UNOFFICIAL, DEFAULT_ADDRESS, null, GroupJoinType.PASSWORD, null);
	}

	public static AdminGroupCreateRequest createRequest(String name, String logoImageFileUuid) {
		return new AdminGroupCreateRequest(
			name, logoImageFileUuid, GroupType.UNOFFICIAL, DEFAULT_ADDRESS, null, GroupJoinType.PASSWORD, null);
	}
}
