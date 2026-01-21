package com.tasteam.domain.file.dto.request;

import java.util.List;

import com.tasteam.domain.file.entity.FilePurpose;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PresignedUploadRequest(
	@NotNull(message = "purpose는 필수입니다")
	FilePurpose purpose,
	@NotEmpty(message = "files는 최소 1개 이상이어야 합니다") @Size(max = 5, message = "files는 최대 5개까지 허용됩니다") @Valid
	List<PresignedUploadFileRequest> files) {
}
