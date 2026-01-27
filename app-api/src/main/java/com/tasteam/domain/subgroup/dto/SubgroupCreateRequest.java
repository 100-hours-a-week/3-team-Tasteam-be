package com.tasteam.domain.subgroup.dto;

import com.tasteam.domain.subgroup.type.SubgroupJoinType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubgroupCreateRequest {

	@NotBlank
	private String name;

	private String description;

	private String profileImageUrl;

	@NotNull
	private SubgroupJoinType joinType;

	private String password;
}
