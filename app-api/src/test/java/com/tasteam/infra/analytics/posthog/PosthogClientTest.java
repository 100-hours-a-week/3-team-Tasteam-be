package com.tasteam.infra.analytics.posthog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@UnitTest
@DisplayName("PostHog 클라이언트")
class PosthogClientTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	@Test
	@DisplayName("capture 요청의 distinct_id는 event_id로 고정된다")
	void capture_usesEventIdAsDistinctId() throws Exception {
		// given
		try (MockWebServer server = new MockWebServer()) {
			server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

			PosthogProperties properties = new PosthogProperties();
			properties.setHost(server.url("/").toString());
			properties.setApiKey("phc_test_key");
			RestClient restClient = RestClient.builder()
				.baseUrl(properties.getHost())
				.build();
			PosthogClient client = new PosthogClient(restClient, properties);

			ActivityEvent event = new ActivityEvent(
				"evt-1",
				"review.created",
				"v1",
				Instant.parse("2026-02-19T00:00:00Z"),
				10L,
				null,
				Map.of("restaurantId", 99L));

			// when
			client.capture(event);

			// then
			RecordedRequest request = server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getPath()).isEqualTo("/capture/");
			Map<String, Object> body = objectMapper.readValue(request.getBody().readUtf8(), Map.class);
			assertThat(body.get("api_key")).isEqualTo("phc_test_key");
			assertThat(body.get("event")).isEqualTo("review.created");
			assertThat(body.get("distinct_id")).isEqualTo("evt-1");
			Map<String, Object> propertiesBody = (Map<String, Object>)body.get("properties");
			assertThat(propertiesBody.get("event_id")).isEqualTo("evt-1");
			assertThat(((Number)propertiesBody.get("restaurantId")).longValue()).isEqualTo(99L);
		}
	}

	@Test
	@DisplayName("API Key가 비어 있으면 capture를 수행하지 않는다")
	void capture_throwsWhenApiKeyMissing() {
		// given
		PosthogProperties properties = new PosthogProperties();
		properties.setHost("http://localhost");
		properties.setApiKey(" ");
		RestClient restClient = RestClient.builder()
			.baseUrl(properties.getHost())
			.build();
		PosthogClient client = new PosthogClient(restClient, properties);
		ActivityEvent event = new ActivityEvent(
			"evt-1",
			"review.created",
			"v1",
			Instant.parse("2026-02-19T00:00:00Z"),
			10L,
			null,
			Map.of());

		// when & then
		assertThatThrownBy(() -> client.capture(event))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("PostHog API Key");
	}
}
