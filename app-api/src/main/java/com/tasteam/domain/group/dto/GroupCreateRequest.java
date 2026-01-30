package com.tasteam.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupCreateRequest(
	@NotBlank
	String name,
	@NotBlank @JsonProperty("logoImageURL")
	String logoImageUrl,
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
