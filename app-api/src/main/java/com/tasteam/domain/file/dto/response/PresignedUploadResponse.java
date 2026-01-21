package com.tasteam.domain.file.dto.response;

import java.util.List;

public record PresignedUploadResponse(List<PresignedUploadItem> uploads) {
}
