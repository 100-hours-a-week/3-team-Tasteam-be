package com.tasteam.domain.report.dto.response;

import java.time.Instant;

public record ReportCreateResponse(Long id, Instant createdAt) {
}
