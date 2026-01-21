package com.tasteam.domain.file.dto.response;

import java.time.Instant;
import java.util.List;

public record ImageDetailResponse(
	String fileUuid,
	String status,
	String purpose,
	Instant createdAt,
	List<LinkedDomainResponse> linkedDomains) {
}
