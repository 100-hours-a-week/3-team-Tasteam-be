package com.tasteam.domain.group.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupEmailAuthenticationRequest {

	@NotBlank
	private String code;
}
