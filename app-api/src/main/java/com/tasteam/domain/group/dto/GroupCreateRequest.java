package com.tasteam.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tasteam.domain.group.entity.GroupJoinType;
import com.tasteam.domain.group.entity.GroupType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateRequest {

	@NotBlank
	private String name;

	@NotBlank
	@JsonProperty("logoImageURL")
	private String logoImageUrl;

	@NotNull
	private GroupType type;

	@NotBlank
	private String address;

	private String detailAddress;

	@NotNull
	@Valid
	private Location location;

	@NotNull
	private GroupJoinType joinType;

	private String emailDomain;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Location {
		@NotNull
		private Double latitude;

		@NotNull
		private Double longitude;
	}
}
