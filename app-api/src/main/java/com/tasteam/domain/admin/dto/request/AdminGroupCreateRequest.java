package com.tasteam.domain.admin.dto.request;

import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupType;
import com.tasteam.global.validation.ValidationPatterns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminGroupCreateRequest(
	@NotBlank(message = "그룹 이름은 필수입니다") @Size(max = 100, message = "그룹 이름은 최대 100자입니다")
	String name,
	@Pattern(regexp = ValidationPatterns.UUID_PATTERN, message = "logoImageFileUuid 형식이 올바르지 않습니다")
	String logoImageFileUuid,

	@NotNull(message = "그룹 타입은 필수입니다")
	GroupType type,

	@NotBlank(message = "주소는 필수입니다") @Size(max = 255, message = "주소는 최대 255자입니다")
	String address,

	@Size(max = 255, message = "상세 주소는 최대 255자입니다")
	String detailAddress,

	@NotNull(message = "가입 타입은 필수입니다")
	GroupJoinType joinType,

	@Size(max = 100, message = "이메일 도메인은 최대 100자입니다")
	String emailDomain) {
}
