package com.tasteam.domain.file.dto.request;

import java.util.List;

import com.tasteam.domain.file.entity.FilePurpose;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PresignedUploadRequest(
	@Schema(description = "업로드 목적", example = "REVIEW_IMAGE") @NotNull(message = "purpose는 필수입니다")
	FilePurpose purpose,
	@Schema(description = "업로드 파일 메타데이터 목록") @NotEmpty(message = "files는 최소 1개 이상이어야 합니다") @Size(max = 5, message = "files는 최대 5개까지 허용됩니다") @Valid
	List<PresignedUploadFileRequest> files) {
}
