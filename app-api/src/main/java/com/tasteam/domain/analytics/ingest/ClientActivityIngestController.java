package com.tasteam.domain.analytics.ingest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.analytics.ingest.docs.ClientActivityIngestControllerDocs;
import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventsIngestRequest;
import com.tasteam.domain.analytics.ingest.dto.response.ClientActivityEventsIngestResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analytics/events")
@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientActivityIngestController implements ClientActivityIngestControllerDocs {

	private final ClientActivityIngestService clientActivityIngestService;

	@Override
	@PostMapping
	public SuccessResponse<ClientActivityEventsIngestResponse> ingest(
		@CurrentUser
		Long memberId,
		@RequestHeader(value = "X-Anonymous-Id", required = false)
		String headerAnonymousId,
		@RequestBody @Valid
		ClientActivityEventsIngestRequest request) {
		String resolvedAnonymousId = resolveAnonymousId(request.anonymousId(), headerAnonymousId);
		int acceptedCount = clientActivityIngestService.ingest(memberId, resolvedAnonymousId, request.events());
		return SuccessResponse.success(new ClientActivityEventsIngestResponse(acceptedCount));
	}

	private String resolveAnonymousId(String requestAnonymousId, String headerAnonymousId) {
		if (StringUtils.hasText(requestAnonymousId)) {
			return requestAnonymousId;
		}
		return headerAnonymousId;
	}
}
