package com.tasteam.domain.analytics.ingest.docs;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventsIngestRequest;
import com.tasteam.domain.analytics.ingest.dto.response.ClientActivityEventsIngestResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@SwaggerTagOrder(43)
@Tag(name = "Analytics", description = "사용자 활동 분석 API")
public interface ClientActivityIngestControllerDocs {

	@Operation(summary = "클라이언트 활동 이벤트 수집", description = "클라이언트에서 발생한 사용자 활동 이벤트를 일괄 수집합니다.")
	SuccessResponse<ClientActivityEventsIngestResponse> ingest(
		@CurrentUser
		Long memberId,
		@Parameter(description = "익명 사용자 ID", hidden = true) @RequestHeader(value = "X-Anonymous-Id", required = false)
		String headerAnonymousId,
		@RequestBody @Valid
		ClientActivityEventsIngestRequest request);
}
