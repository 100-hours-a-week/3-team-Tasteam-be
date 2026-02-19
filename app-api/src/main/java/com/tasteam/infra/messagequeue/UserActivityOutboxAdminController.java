package com.tasteam.infra.messagequeue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.analytics.resilience.UserActivityReplayService;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/user-activity/outbox")
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class UserActivityOutboxAdminController {

	private static final int MAX_LIMIT = 500;

	private final UserActivitySourceOutboxService outboxService;
	private final UserActivityReplayService replayService;

	@GetMapping("/summary")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<UserActivityOutboxSummaryResponse> getSummary() {
		return SuccessResponse.success(UserActivityOutboxSummaryResponse.from(outboxService.summarize()));
	}

	@PostMapping("/replay")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<UserActivityReplayResponse> replay(
		@RequestParam(defaultValue = "100")
		int limit) {
		int validatedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
		return SuccessResponse.success(UserActivityReplayResponse.from(replayService.replayPending(validatedLimit)));
	}
}
