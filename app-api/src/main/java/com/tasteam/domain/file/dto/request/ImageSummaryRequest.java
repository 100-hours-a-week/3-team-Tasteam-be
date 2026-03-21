package com.tasteam.domain.file.dto.request;

import java.util.List;

import com.tasteam.global.validation.ValidationPatterns;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ImageSummaryRequest(
	@NotEmpty(message = "fileUuids는 최소 1개 이상이어야 합니다") @Size(max = 100, message = "fileUuids는 최대 100개까지 허용됩니다")
	List<@Pattern(regexp = ValidationPatterns.UUID_PATTERN, message = "fileUuid 형식이 올바르지 않습니다") String> fileUuids) {
}
