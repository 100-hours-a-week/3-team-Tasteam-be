package com.tasteam.fixture;

import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;

public final class SubgroupRequestFixture {

	public static final String DEFAULT_NAME = "테스트 하위그룹";

	private SubgroupRequestFixture() {}

	public static SubgroupCreateRequest createRequest(String name) {
		return new SubgroupCreateRequest(name, null, null, SubgroupJoinType.OPEN, null);
	}

	public static SubgroupCreateRequest createRequest(String name, String profileImageFileUuid) {
		return new SubgroupCreateRequest(name, null, profileImageFileUuid, SubgroupJoinType.OPEN, null);
	}
}
