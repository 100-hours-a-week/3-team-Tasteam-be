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
import com.tasteam.domain.admin.controller.AdminAnnouncementController;
import com.tasteam.domain.admin.controller.AdminAuthController;
import com.tasteam.domain.admin.controller.AdminDummyController;
import com.tasteam.domain.admin.controller.AdminFoodCategoryController;
import com.tasteam.domain.admin.controller.AdminGeocodingController;
import com.tasteam.domain.admin.controller.AdminGroupController;
import com.tasteam.domain.admin.controller.AdminJobController;
import com.tasteam.domain.admin.controller.AdminMenuController;
import com.tasteam.domain.admin.controller.AdminPromotionController;
import com.tasteam.domain.admin.controller.AdminReportController;
import com.tasteam.domain.admin.controller.AdminRestaurantController;
import com.tasteam.domain.admin.controller.AdminReviewController;
import com.tasteam.domain.admin.controller.AdminScheduleController;
import com.tasteam.domain.admin.controller.AdminSpaFallbackController;
import com.tasteam.domain.admin.service.AdminAnnouncementService;
import com.tasteam.domain.admin.service.AdminGroupService;
import com.tasteam.domain.admin.service.AdminPromotionService;
import com.tasteam.domain.admin.service.AdminReportService;
import com.tasteam.domain.admin.service.AdminRestaurantService;
import com.tasteam.domain.admin.service.AdminReviewService;
import com.tasteam.domain.analytics.resilience.UserActivityReplayService;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;
import com.tasteam.domain.notification.controller.AdminNotificationController;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.domain.notification.service.NotificationBroadcastService;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.domain.restaurant.service.FoodCategoryService;
import com.tasteam.domain.restaurant.service.MenuService;
import com.tasteam.domain.restaurant.service.RestaurantScheduleService;
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
	AdminAnnouncementController.class,
	AdminAuthController.class,
	AdminDummyController.class,
	AdminFoodCategoryController.class,
	AdminGeocodingController.class,
	AdminGroupController.class,
	AdminJobController.class,
	AdminMenuController.class,
	AdminPromotionController.class,
	AdminReportController.class,
	AdminRestaurantController.class,
	AdminReviewController.class,
	AdminScheduleController.class,
	AdminSpaFallbackController.class,
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
	protected AdminAnnouncementService adminAnnouncementService;

	@MockitoBean
	protected DummyDataSeedService dummyDataSeedService;

	@MockitoBean
	protected DummySeedJobTracker jobTracker;

	@MockitoBean
	protected FoodCategoryService foodCategoryService;

	@MockitoBean
	protected NaverGeocodingClient naverGeocodingClient;

	@MockitoBean
	protected AdminGroupService adminGroupService;

	@MockitoBean
	protected ImageOptimizationService imageOptimizationService;

	@MockitoBean
	protected com.tasteam.domain.file.service.FileService fileService;

	@MockitoBean
	protected MenuService menuService;

	@MockitoBean
	protected AdminPromotionService adminPromotionService;

	@MockitoBean
	protected AdminReportService adminReportService;

	@MockitoBean
	protected AdminRestaurantService adminRestaurantService;

	@MockitoBean
	protected AdminReviewService adminReviewService;

	@MockitoBean
	protected RestaurantScheduleService restaurantScheduleService;

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
