package com.tasteam.domain.subgroup.dto;

import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.global.validation.ValidationPatterns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

	@Pattern(regexp = ValidationPatterns.UUID_PATTERN, message = "profileImageId 형식이 올바르지 않습니다")
	private String profileImageId;

	@NotNull
	private SubgroupJoinType joinType;

	private String password;
}
