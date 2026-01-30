package com.tasteam.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupType;
import com.tasteam.global.validation.ValidationPatterns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GroupCreateRequest(
	@NotBlank
	String name,
	@NotBlank @JsonProperty("logoImageId") @Pattern(regexp = ValidationPatterns.UUID_PATTERN, message = "logoImageId 형식이 올바르지 않습니다")
	String logoImageId,
	@NotNull
	GroupType type,
	@NotBlank
	String address,
	String detailAddress,
	@NotNull @Valid
	Location location,
	@NotNull
	GroupJoinType joinType,
	String emailDomain,
	String code) {

	public record Location(
		@NotNull
		Double latitude,
		@NotNull
		Double longitude) {
	}
}
