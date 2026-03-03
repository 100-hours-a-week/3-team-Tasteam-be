package com.tasteam.domain.notification.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.notification.dto.request.AdminBroadcastEmailRequest;
import com.tasteam.domain.notification.dto.request.AdminBroadcastPushRequest;
import com.tasteam.domain.notification.dto.request.AdminPushNotificationRequest;
import com.tasteam.domain.notification.dto.response.AdminBroadcastResultResponse;
import com.tasteam.domain.notification.dto.response.AdminPushNotificationResponse;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.domain.notification.service.NotificationBroadcastService;

@ControllerWebMvcTest(AdminNotificationController.class)
@DisplayName("[유닛](Notification) AdminNotificationController 단위 테스트")
class AdminNotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private FcmPushService fcmPushService;

	@MockitoBean
	private NotificationBroadcastService notificationBroadcastService;

	@Nested
	@DisplayName("테스트 푸시 발송")
	class SendTestPush {

		@Test
		@DisplayName("유효한 요청이면 푸시 발송 결과를 반환한다")
		void 테스트푸시_성공() throws Exception {
			// given
			var request = new AdminPushNotificationRequest(
				1L,
				"테스트 제목",
				"테스트 메시지",
				"https://example.com/deeplink");
			given(fcmPushService.sendToMember(request.memberId(), request.title(), request.body(), request.deepLink()))
				.willReturn(new AdminPushNotificationResponse(2, 0, 0));

			// when & then
			mockMvc.perform(post("/api/v1/admin/notifications/push/test")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.successCount").value(2))
				.andExpect(jsonPath("$.data.failureCount").value(0))
				.andExpect(jsonPath("$.data.invalidTokenCount").value(0));
		}

		@Test
		@DisplayName("필수 필드 누락이면 400으로 실패한다")
		void 테스트푸시_요청값_누락_실패() throws Exception {
			// given
			String body = "{}";

			// when & then
			mockMvc.perform(post("/api/v1/admin/notifications/push/test")
				.contentType(APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("푸시 브로드캐스트 발송")
	class BroadcastPush {

		@Test
		@DisplayName("유효한 요청이면 브로드캐스트 푸시 결과를 반환한다")
		void 푸시브로드캐스트_성공() throws Exception {
			// given
			var request = new AdminBroadcastPushRequest(
				NotificationType.NOTICE,
				"공지",
				"새로운 공지사항이 등록되었습니다.",
				"https://example.com/notice");
			given(notificationBroadcastService.broadcastPush(
				request.notificationType(),
				request.title(),
				request.body(),
				request.deepLink())).willReturn(new AdminBroadcastResultResponse(2, 2, 0, 0));

			// when & then
			mockMvc.perform(post("/api/v1/admin/notifications/push/broadcast")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalTargets").value(2))
				.andExpect(jsonPath("$.data.successCount").value(2))
				.andExpect(jsonPath("$.data.failureCount").value(0))
				.andExpect(jsonPath("$.data.skippedCount").value(0));
		}

		@Test
		@DisplayName("알림 타입이 잘못되면 내부 처리 오류로 실패한다")
		void 푸시브로드캐스트_잘못된_요청_실패() throws Exception {
			// given
			String body = "{\"notificationType\":\"INVALID\",\"title\":\"공지\",\"body\":\"본문\"}";

			// when & then
			mockMvc.perform(post("/api/v1/admin/notifications/push/broadcast")
				.contentType(APPLICATION_JSON)
				.content(body))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("이메일 브로드캐스트 발송")
	class BroadcastEmail {

		@Test
		@DisplayName("유효한 요청이면 브로드캐스트 이메일 결과를 반환한다")
		void 이메일브로드캐스트_성공() throws Exception {
			// given
			var request = new AdminBroadcastEmailRequest(
				NotificationType.NOTICE,
				"notice-template",
				Map.of("name", "테스트"));
			given(notificationBroadcastService.broadcastEmail(
				request.notificationType(),
				request.templateKey(),
				request.variables())).willReturn(new AdminBroadcastResultResponse(1, 1, 0, 0));

			// when & then
			mockMvc.perform(post("/api/v1/admin/notifications/email/broadcast")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalTargets").value(1))
				.andExpect(jsonPath("$.data.successCount").value(1))
				.andExpect(jsonPath("$.data.failureCount").value(0))
				.andExpect(jsonPath("$.data.skippedCount").value(0));
		}

		@Test
		@DisplayName("필수 값 누락이면 400으로 실패한다")
		void 이메일브로드캐스트_요청값_누락_실패() throws Exception {
			// given
			String body = "{}";

			// when & then
			mockMvc.perform(post("/api/v1/admin/notifications/email/broadcast")
				.contentType(APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}
}
