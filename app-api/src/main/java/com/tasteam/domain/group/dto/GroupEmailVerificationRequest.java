package com.tasteam.domain.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupEmailVerificationRequest {

	@NotBlank
	@Email
	private String email;
}
