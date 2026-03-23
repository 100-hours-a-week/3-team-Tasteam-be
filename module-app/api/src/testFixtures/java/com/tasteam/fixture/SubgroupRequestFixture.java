package com.tasteam.fixture;

import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;

public final class SubgroupRequestFixture {

	public static final String DEFAULT_NAME = "테스트 하위그룹";
	public static final String DEFAULT_DESCRIPTION = "테스트 하위그룹 설명";

	private SubgroupRequestFixture() {}

	public static SubgroupCreateRequest createRequest() {
		return new SubgroupCreateRequest(DEFAULT_NAME, DEFAULT_DESCRIPTION, null, SubgroupJoinType.OPEN, null);
	}

	public static SubgroupCreateRequest createRequest(String name) {
		return new SubgroupCreateRequest(name, DEFAULT_DESCRIPTION, null, SubgroupJoinType.OPEN, null);
	}

	public static SubgroupCreateRequest createRequest(String name, String description) {
		return new SubgroupCreateRequest(name, description, null, SubgroupJoinType.OPEN, null);
	}

	public static SubgroupCreateRequest createRequestWithImage(String name, String profileImageFileUuid) {
		return new SubgroupCreateRequest(name, DEFAULT_DESCRIPTION, profileImageFileUuid, SubgroupJoinType.OPEN, null);
	}

	public static SubgroupCreateRequest createPasswordRequest(String name, String password) {
		return new SubgroupCreateRequest(name, DEFAULT_DESCRIPTION, null, SubgroupJoinType.PASSWORD, password);
	}
}
