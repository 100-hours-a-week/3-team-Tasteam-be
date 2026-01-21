package com.tasteam.domain.file.dto.response;

import java.time.Instant;

public record LinkedDomainResponse(
	String domainType,
	Long domainId,
	Integer sortOrder,
	Instant linkedAt) {
}
