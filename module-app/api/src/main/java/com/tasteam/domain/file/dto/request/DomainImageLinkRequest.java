package com.tasteam.domain.file.dto.request;

import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.global.validation.ValidationPatterns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record DomainImageLinkRequest(
	@NotNull(message = "domainType은 필수입니다")
	DomainType domainType,
	@Positive(message = "domainId는 양수여야 합니다")
	Long domainId,
	@NotBlank(message = "fileUuid는 필수입니다") @Pattern(regexp = ValidationPatterns.UUID_PATTERN, message = "fileUuid 형식이 올바르지 않습니다")
	String fileUuid,
	@PositiveOrZero(message = "sortOrder는 0 이상이어야 합니다")
	Integer sortOrder) {
}
