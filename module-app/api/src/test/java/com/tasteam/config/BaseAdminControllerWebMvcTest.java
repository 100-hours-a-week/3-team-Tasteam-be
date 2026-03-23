package com.tasteam.config;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.batch.dummy.DummySeedJobTracker;
import com.tasteam.batch.dummy.service.DummyDataSeedService;
import com.tasteam.batch.image.optimization.service.ImageOptimizationService;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.admin.controller.AdminDummyController;
import com.tasteam.domain.admin.controller.AdminJobController;
import com.tasteam.domain.analytics.resilience.UserActivityReplayService;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;
import com.tasteam.domain.notification.controller.AdminNotificationController;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.domain.notification.service.NotificationBroadcastService;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.infra.messagequeue.UserActivityOutboxAdminController;
import com.tasteam.infra.messagequeue.trace.MessageQueueTraceAdminController;
import com.tasteam.infra.messagequeue.trace.MessageQueueTraceService;

@TestPropertySource(properties = {
	"tasteam.admin.username=admin",
	"tasteam.admin.password=pass1234!",
	"tasteam.message-queue.enabled=true"
})
@ControllerWebMvcTest({
	AdminDummyController.class,
	AdminJobController.class,
	AdminNotificationController.class,
	MessageQueueTraceAdminController.class,
	UserActivityOutboxAdminController.class
})
@DisplayName("[유닛](Base) AdminControllerWebMvc 단위 테스트")
public abstract class BaseAdminControllerWebMvcTest {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected JwtTokenProvider jwtTokenProvider;

	@Autowired
	protected JwtCookieProvider jwtCookieProvider;

	@MockitoBean
	protected DummyDataSeedService dummyDataSeedService;

	@MockitoBean
	protected DummySeedJobTracker jobTracker;

	@MockitoBean
	protected ImageOptimizationService imageOptimizationService;

	@MockitoBean
	protected com.tasteam.domain.file.service.FileService fileService;

	@MockitoBean
	protected FcmPushService fcmPushService;

	@MockitoBean
	protected NotificationBroadcastService notificationBroadcastService;

	@MockitoBean
	protected MessageQueueTraceService traceService;

	@MockitoBean
	protected UserActivitySourceOutboxService outboxService;

	@MockitoBean
	protected UserActivityReplayService replayService;
}
