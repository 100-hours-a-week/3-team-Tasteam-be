package com.tasteam.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminFoodCategoryCreateRequest(
	@NotBlank(message = "카테고리 이름은 필수입니다") @Size(max = 20, message = "카테고리 이름은 최대 20자입니다")
	String name) {
}
