package com.tasteam.domain.analytics.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventItemRequest;
import com.tasteam.domain.analytics.ingest.dto.request.ClientActivityEventsIngestRequest;
import com.tasteam.domain.analytics.ingest.dto.response.ClientActivityEventsIngestResponse;
import com.tasteam.global.dto.api.SuccessResponse;

@UnitTest
@DisplayName("클라이언트 활동 이벤트 수집 컨트롤러")
class ClientActivityIngestControllerTest {

	@Test
	@DisplayName("요청 anonymousId가 없으면 헤더 anonymousId를 사용해 수집을 호출한다")
	void ingest_usesHeaderAnonymousIdWhenRequestAnonymousIdMissing() {
		// given
		ClientActivityIngestService service = mock(ClientActivityIngestService.class);
		ClientActivityIngestController controller = new ClientActivityIngestController(service);
		ClientActivityEventsIngestRequest request = new ClientActivityEventsIngestRequest(
			null,
			List.of(new ClientActivityEventItemRequest(
				"evt-1",
				"ui.restaurant.viewed",
				"v1",
				Instant.parse("2026-02-19T00:00:00Z"),
				Map.of())));
		when(service.ingest(null, "anon-header", request.events())).thenReturn(1);

		// when
		SuccessResponse<ClientActivityEventsIngestResponse> response = controller.ingest(
			null,
			"anon-header",
			request);

		// then
		verify(service).ingest(null, "anon-header", request.events());
		assertThat(response.getData()).isNotNull();
		assertThat(response.getData().acceptedCount()).isEqualTo(1);
	}
}
