package com.tasteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.admin.dto.request.AdminRawDataExportRequest;
import com.tasteam.domain.analytics.export.RawDataExportAsyncLauncher;
import com.tasteam.domain.analytics.export.RawDataType;

@ControllerWebMvcTest(AdminRawDataExportController.class)
@DisplayName("[유닛](Analytics) AdminRawDataExportController 단위 테스트")
class AdminRawDataExportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RawDataExportAsyncLauncher rawDataExportAsyncLauncher;

	@Test
	@DisplayName("로그인 사용자 raw export 요청을 접수하면 202를 반환한다")
	void raw_export_요청_성공() throws Exception {
		AdminRawDataExportRequest request = new AdminRawDataExportRequest(
			LocalDate.of(2026, 3, 24),
			Set.of(RawDataType.RESTAURANTS),
			"manual-raw-export");

		mockMvc.perform(post("/api/v1/analytics/raw-exports")
			.contentType("application/json")
			.content(objectMapper.writeValueAsBytes(request)))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.requestId").value("manual-raw-export"))
			.andExpect(jsonPath("$.data.dt").value("2026-03-24"))
			.andExpect(jsonPath("$.data.targets[0]").value("RESTAURANTS"));

		then(rawDataExportAsyncLauncher).should().launch(any());
	}

	@Test
	@DisplayName("기존 admin 경로로도 raw export 요청을 접수한다")
	void admin_경로도_지원() throws Exception {
		mockMvc.perform(post("/api/v1/admin/analytics/raw-exports"))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.success").value(true));

		then(rawDataExportAsyncLauncher).should().launch(any());
	}
}
