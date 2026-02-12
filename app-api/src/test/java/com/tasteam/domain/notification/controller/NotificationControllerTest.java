package com.tasteam.domain.notification.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.notification.dto.response.NotificationPreferenceResponse;
import com.tasteam.domain.notification.dto.response.NotificationResponse;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.NotificationPreferenceService;
import com.tasteam.domain.notification.service.NotificationService;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;

@ControllerWebMvcTest(NotificationController.class)
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationService notificationService;

	@MockitoBean
	private NotificationPreferenceService notificationPreferenceService;

	private OffsetPageResponse<NotificationResponse> createMockNotifications() {
		NotificationResponse notification = new NotificationResponse(
			1L,
			NotificationType.SYSTEM,
			"시스템 점검 안내",
			"2월 15일 새벽 점검이 진행됩니다.",
			"/notices/1",
			Instant.parse("2026-02-10T10:00:00Z"),
			null);

		return new OffsetPageResponse<>(
			List.of(notification),
			new OffsetPagination(0, 10, 1, 1));
	}

	private List<NotificationPreferenceResponse> createMockPreferences() {
		return List.of(
			new NotificationPreferenceResponse(NotificationChannel.WEB, NotificationType.SYSTEM, true),
			new NotificationPreferenceResponse(NotificationChannel.PUSH, NotificationType.CHAT, true));
	}

	@Nested
	@DisplayName("알림 목록 조회")
	class GetNotifications {

		@Test
		@DisplayName("알림 목록을 조회하면 페이징된 결과를 반환한다")
		void getNotifications_returnsPagedResult() throws Exception {
			given(notificationService.getNotifications(anyLong(), anyInt(), anyInt()))
				.willReturn(createMockNotifications());

			mockMvc.perform(get("/api/v1/members/me/notifications")
				.param("page", "0")
				.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].notificationType").value("SYSTEM"))
				.andExpect(jsonPath("$.data.items[0].title").value("시스템 점검 안내"))
				.andExpect(jsonPath("$.data.pagination.page").value(0))
				.andExpect(jsonPath("$.data.pagination.size").value(10));
		}
	}

	@Nested
	@DisplayName("알림 선호도 조회")
	class GetPreferences {

		@Test
		@DisplayName("알림 선호도를 조회하면 목록을 반환한다")
		void getPreferences_returnsList() throws Exception {
			given(notificationPreferenceService.getPreferences(anyLong()))
				.willReturn(createMockPreferences());

			mockMvc.perform(get("/api/v1/members/me/notification-preferences"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].channel").value("WEB"))
				.andExpect(jsonPath("$.data[0].notificationType").value("SYSTEM"))
				.andExpect(jsonPath("$.data[0].isEnabled").value(true));
		}
	}
}
