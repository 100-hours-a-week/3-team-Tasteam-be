package com.tasteam.global.health.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;

@ControllerWebMvcTest(HealthCheckController.class)
@DisplayName("[유닛](Health) HealthCheckController 단위 테스트")
class HealthCheckControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HealthEndpoint healthEndpoint;

	@Test
	@DisplayName("헬스체크 API가 상태 정보를 반환한다")
	void 헬스체크_성공() throws Exception {
		// given
		HealthComponent health = Health.up()
			.withDetails(Map.of("redis", "ok", "database", "connected"))
			.build();
		given(healthEndpoint.health()).willReturn(health);

		// when & then
		mockMvc.perform(get("/api/v1/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("UP"));
	}

	@Test
	@DisplayName("헬스체크 수행 중 예외가 발생하면 500으로 실패한다")
	void 헬스체크_예외_실패() throws Exception {
		// given
		given(healthEndpoint.health()).willThrow(new RuntimeException("health endpoint down"));

		// when & then
		mockMvc.perform(get("/api/v1/health"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.success").value(false));
	}
}
