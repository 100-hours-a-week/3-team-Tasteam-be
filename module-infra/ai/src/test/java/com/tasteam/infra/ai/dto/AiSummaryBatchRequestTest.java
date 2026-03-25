package com.tasteam.infra.ai.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("[유닛](Infra) AiSummaryBatchRequest 단위 테스트")
class AiSummaryBatchRequestTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("배치 요약 요청은 restaurants를 레스토랑 ID 배열로 직렬화한다")
	void serializesRestaurantsAsIdArray() throws Exception {
		AiSummaryBatchRequest request = new AiSummaryBatchRequest(java.util.List.of(1L, 2L));

		String json = objectMapper.writeValueAsString(request);

		assertThat(json).isEqualTo("{\"restaurants\":[1,2]}");
	}

	@Test
	@DisplayName("단건 helper는 레스토랑 ID 하나만 담는다")
	void singleRestaurant_buildsIdOnlyPayload() {
		AiSummaryBatchRequest request = AiSummaryBatchRequest.singleRestaurant(123L);

		assertThat(request.restaurants()).containsExactly(123L);
	}
}
