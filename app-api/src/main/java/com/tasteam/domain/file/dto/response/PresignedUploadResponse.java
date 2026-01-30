package com.tasteam.domain.file.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Presigned 업로드 생성 응답")
public record PresignedUploadResponse(
	@Schema(description = "Presigned 업로드 정보 목록")
	List<PresignedUploadItem> uploads) {
}
