package com.tasteam.domain.file.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PresignedUploadFileRequest(
	@NotBlank(message = "fileName은 필수입니다") @Size(max = 256, message = "fileName 길이는 256자 이하여야 합니다")
	String fileName,
	@NotBlank(message = "contentType은 필수입니다") @Pattern(regexp = "^image/.+$", message = "contentType은 image/* 형식이어야 합니다")
	String contentType,
	@Min(value = 1, message = "size는 1바이트 이상이어야 합니다") @Max(value = 10485760, message = "size는 10MB 이하여야 합니다")
	long size) {
}
