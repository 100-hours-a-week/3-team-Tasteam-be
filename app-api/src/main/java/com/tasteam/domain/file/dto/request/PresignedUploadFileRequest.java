package com.tasteam.domain.file.dto.request;

import com.tasteam.domain.file.validation.AllowedContentType;
import com.tasteam.domain.file.validation.UploadFileSize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresignedUploadFileRequest(
	@Schema(description = "원본 파일명", example = "a.jpg") @NotBlank(message = "fileName은 필수입니다") @Size(max = 256, message = "fileName 길이는 256자 이하여야 합니다")
	String fileName,
	@Schema(description = "MIME 타입", example = "image/jpeg") @NotBlank(message = "contentType은 필수입니다") @AllowedContentType
	String contentType,
	@Schema(description = "파일 크기(bytes)", example = "345123") @UploadFileSize
	long size) {
}
