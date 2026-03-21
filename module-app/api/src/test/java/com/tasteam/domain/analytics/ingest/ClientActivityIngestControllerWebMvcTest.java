package com.tasteam.domain.analytics.ingest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseControllerWebMvcTest;
import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventItemRequest;
import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventsIngestRequest;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AnalyticsErrorCode;

@DisplayName("[유닛](Analytics) ClientActivityIngestController 단위 테스트")
class ClientActivityIngestControllerWebMvcTest extends BaseControllerWebMvcTest {

	@Test
	@DisplayName("이벤트 수집에 성공하면 수집 건수를 반환한다")
	void 이벤트_수집_성공() throws Exception {
		// given
		ClientActivityEventsIngestRequest request = new ClientActivityEventsIngestRequest(
			"anon-1",
			List.of(new ClientActivityEventItemRequest(
				"evt-1",
				"ui.restaurant.viewed",
				"v1",
				Instant.parse("2026-02-01T12:00:00Z"),
				Map.of("source", "mobile"))));
		given(clientActivityIngestService.ingest(anyLong(), anyString(), any()))
			.willReturn(1);

		// when & then
		mockMvc.perform(post("/api/v1/analytics/events")
			.header("X-Anonymous-Id", "header-anon")
			.contentType(APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.acceptedCount").value(1));
	}

	@Test
	@DisplayName("이벤트가 비어 있으면 400으로 실패한다")
	void 이벤트_수집_빈요청_실패() throws Exception {
		// given
		ClientActivityEventsIngestRequest request = new ClientActivityEventsIngestRequest("anon-1", List.of());

		// when & then
		mockMvc.perform(post("/api/v1/analytics/events")
			.contentType(APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("수집 제한을 초과하면 429로 실패한다")
	void 이벤트_수집_레이트리밋_실패() throws Exception {
		// given
		ClientActivityEventsIngestRequest request = new ClientActivityEventsIngestRequest(
			"anon-1",
			List.of(new ClientActivityEventItemRequest(
				"evt-1",
				"ui.restaurant.viewed",
				"v1",
				Instant.parse("2026-02-01T12:00:00Z"),
				Map.of("source", "mobile"))));
		given(clientActivityIngestService.ingest(anyLong(), anyString(), any()))
			.willThrow(new BusinessException(AnalyticsErrorCode.ANALYTICS_INGEST_RATE_LIMIT_EXCEEDED));

		// when & then
		mockMvc.perform(post("/api/v1/analytics/events")
			.contentType(APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("ANALYTICS_INGEST_RATE_LIMIT_EXCEEDED"));
	}
}
