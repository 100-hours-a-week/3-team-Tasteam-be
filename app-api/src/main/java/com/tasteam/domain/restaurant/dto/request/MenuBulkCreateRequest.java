package com.tasteam.domain.restaurant.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record MenuBulkCreateRequest(
	@NotEmpty @Valid
	List<MenuCreateRequest> menus) {
}
