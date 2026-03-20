package com.tasteam.infra.ai.dto;

import java.util.List;

public record AiSummaryBatchResponse(
	List<AiSummaryDisplayResponse> results) {
}
