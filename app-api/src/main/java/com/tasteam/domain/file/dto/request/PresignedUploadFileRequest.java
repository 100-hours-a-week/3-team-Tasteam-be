package com.tasteam.domain.file.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PresignedUploadFileRequest(
	@Schema(description = "원본 파일명", example = "a.jpg") @NotBlank(message = "fileName은 필수입니다") @Size(max = 256, message = "fileName 길이는 256자 이하여야 합니다")
	String fileName,
	@Schema(description = "MIME 타입", example = "image/jpeg") @NotBlank(message = "contentType은 필수입니다") @Pattern(regexp = "^image/.+$", message = "contentType은 image/* 형식이어야 합니다")
	String contentType,
	@Schema(description = "파일 크기(bytes)", example = "345123") @Min(value = 1, message = "size는 1바이트 이상이어야 합니다") @Max(value = 10485760, message = "size는 10MB 이하여야 합니다")
	long size) {
}
