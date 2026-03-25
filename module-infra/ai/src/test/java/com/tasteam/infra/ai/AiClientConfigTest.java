package com.tasteam.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("[유닛](Infra) AiClientConfig 단위 테스트")
class AiClientConfigTest {

	@Test
	@DisplayName("baseUrl 끝에 ai prefix가 없으면 /ai를 붙인다")
	void normalizeBaseUrl_appendsAiPrefix() {
		assertThat(AiClientConfig.normalizeBaseUrl("http://fastapi-svc"))
			.isEqualTo("http://fastapi-svc/ai");
		assertThat(AiClientConfig.normalizeBaseUrl("http://fastapi-svc/"))
			.isEqualTo("http://fastapi-svc/ai");
	}

	@Test
	@DisplayName("baseUrl에 이미 ai prefix가 있으면 중복으로 붙이지 않는다")
	void normalizeBaseUrl_keepsExistingAiPrefix() {
		assertThat(AiClientConfig.normalizeBaseUrl("http://fastapi-svc/ai"))
			.isEqualTo("http://fastapi-svc/ai");
		assertThat(AiClientConfig.normalizeBaseUrl("http://fastapi-svc/ai/"))
			.isEqualTo("http://fastapi-svc/ai");
	}
}
