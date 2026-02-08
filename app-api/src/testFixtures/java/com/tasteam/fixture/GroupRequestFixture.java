package com.tasteam.fixture;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationRequest;
import com.tasteam.domain.group.dto.GroupEmailVerificationRequest;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationRequest;
import com.tasteam.domain.group.dto.GroupUpdateRequest;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupType;

public final class GroupRequestFixture {

	public static final String DEFAULT_NAME = "테스트그룹";
	public static final String DEFAULT_LOGO_URL = "https://example.com/logo.jpg";
	public static final String DEFAULT_ADDRESS = "서울시 강남구 테헤란로 1";
	public static final String DEFAULT_EMAIL_DOMAIN = "test.com";
	public static final String DEFAULT_CODE = "123456";
	public static final String DEFAULT_EMAIL = "user@test.com";

	private GroupRequestFixture() {}

	public static GroupCreateRequest createGroupRequest() {
		return new GroupCreateRequest(
			DEFAULT_NAME,
			DEFAULT_LOGO_URL,
			GroupType.OFFICIAL,
			DEFAULT_ADDRESS,
			null,
			new GroupCreateRequest.Location(37.5665, 126.9780),
			GroupJoinType.EMAIL,
			DEFAULT_EMAIL_DOMAIN,
			null);
	}

	public static GroupCreateRequest createPasswordGroupRequest() {
		return new GroupCreateRequest(
			DEFAULT_NAME,
			DEFAULT_LOGO_URL,
			GroupType.UNOFFICIAL,
			DEFAULT_ADDRESS,
			null,
			new GroupCreateRequest.Location(37.5665, 126.9780),
			GroupJoinType.PASSWORD,
			null,
			DEFAULT_CODE);
	}

	public static GroupCreateRequest createEmailGroupRequest(String name, String emailDomain) {
		return new GroupCreateRequest(
			name,
			null,
			GroupType.UNOFFICIAL,
			DEFAULT_ADDRESS,
			null,
			new GroupCreateRequest.Location(37.5, 127.0),
			GroupJoinType.EMAIL,
			emailDomain,
			null);
	}

	public static GroupCreateRequest createEmailGroupRequestWithLogo(String name, String logoUuid, String emailDomain) {
		return new GroupCreateRequest(
			name,
			logoUuid,
			GroupType.UNOFFICIAL,
			DEFAULT_ADDRESS,
			null,
			new GroupCreateRequest.Location(37.5, 127.0),
			GroupJoinType.EMAIL,
			emailDomain,
			null);
	}

	public static GroupCreateRequest createPasswordGroupRequestWithLogo(String name, String logoUuid, String password) {
		return new GroupCreateRequest(
			name,
			logoUuid,
			GroupType.UNOFFICIAL,
			DEFAULT_ADDRESS,
			null,
			new GroupCreateRequest.Location(37.5, 127.0),
			GroupJoinType.PASSWORD,
			null,
			password);
	}

	public static GroupCreateRequest createPasswordGroupRequest(String name, String password) {
		return new GroupCreateRequest(
			name,
			null,
			GroupType.UNOFFICIAL,
			DEFAULT_ADDRESS,
			null,
			new GroupCreateRequest.Location(37.5, 127.0),
			GroupJoinType.PASSWORD,
			null,
			password);
	}

	public static GroupUpdateRequest createUpdateRequest() {
		return new GroupUpdateRequest(
			JsonNodeFactory.instance.textNode("수정된그룹"),
			null,
			null,
			null,
			null,
			null);
	}

	public static GroupEmailVerificationRequest createEmailVerificationRequest() {
		return new GroupEmailVerificationRequest(DEFAULT_EMAIL);
	}

	public static GroupEmailAuthenticationRequest createEmailAuthenticationRequest() {
		return new GroupEmailAuthenticationRequest(DEFAULT_CODE);
	}

	public static GroupPasswordAuthenticationRequest createPasswordAuthenticationRequest() {
		return new GroupPasswordAuthenticationRequest(DEFAULT_CODE);
	}
}
