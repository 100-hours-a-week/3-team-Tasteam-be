package com.tasteam.config;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.analytics.ingest.ClientActivityIngestController;
import com.tasteam.domain.analytics.ingest.ClientActivityIngestService;
import com.tasteam.domain.announcement.controller.AnnouncementController;
import com.tasteam.domain.announcement.service.AnnouncementService;
import com.tasteam.domain.auth.controller.AuthController;
import com.tasteam.domain.auth.service.TokenRefreshService;
import com.tasteam.domain.chat.controller.ChatController;
import com.tasteam.domain.chat.service.ChatService;
import com.tasteam.domain.favorite.service.FavoriteService;
import com.tasteam.domain.file.controller.FileController;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.controller.GroupController;
import com.tasteam.domain.group.service.GroupFacade;
import com.tasteam.domain.location.controller.GeocodeController;
import com.tasteam.domain.location.service.GeocodeService;
import com.tasteam.domain.main.controller.MainController;
import com.tasteam.domain.main.service.MainService;
import com.tasteam.domain.member.controller.MemberController;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.notification.controller.NotificationController;
import com.tasteam.domain.notification.service.NotificationPreferenceService;
import com.tasteam.domain.notification.service.NotificationService;
import com.tasteam.domain.promotion.controller.PromotionController;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.domain.report.controller.ReportController;
import com.tasteam.domain.report.service.ReportService;
import com.tasteam.domain.restaurant.controller.FoodCategoryController;
import com.tasteam.domain.restaurant.controller.MenuController;
import com.tasteam.domain.restaurant.controller.RestaurantController;
import com.tasteam.domain.restaurant.service.FoodCategoryService;
import com.tasteam.domain.restaurant.service.MenuService;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.controller.ReviewController;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.search.controller.RecentSearchController;
import com.tasteam.domain.search.controller.SearchController;
import com.tasteam.domain.search.service.SearchService;
import com.tasteam.domain.subgroup.controller.SubgroupController;
import com.tasteam.domain.subgroup.service.SubgroupFacade;
import com.tasteam.domain.test.controller.WebhookTestController;
import com.tasteam.global.health.controller.HealthCheckController;
import com.tasteam.global.ratelimit.ClientIpResolver;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.jwt.repository.RefreshTokenStore;
import com.tasteam.infra.storage.StorageProperties;

@ControllerWebMvcTest({
	GroupController.class,
	SubgroupController.class,
	ReviewController.class,
	SearchController.class,
	RecentSearchController.class,
	RestaurantController.class,
	MenuController.class,
	MainController.class,
	MemberController.class,
	FileController.class,
	NotificationController.class,
	PromotionController.class,
	AnnouncementController.class,
	AuthController.class,
	ChatController.class,
	ClientActivityIngestController.class,
	FoodCategoryController.class,
	GeocodeController.class,
	ReportController.class,
	HealthCheckController.class,
	WebhookTestController.class
})
@DisplayName("[유닛](Base) BaseControllerWebMvc 단위 테스트")
public abstract class BaseControllerWebMvcTest {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected JwtTokenProvider jwtTokenProvider;

	@Autowired
	protected JwtCookieProvider jwtCookieProvider;

	@MockitoBean
	protected GroupFacade groupFacade;

	@MockitoBean
	protected SubgroupFacade subgroupFacade;

	@MockitoBean
	protected RestaurantService restaurantService;

	@MockitoBean
	protected ReviewService reviewService;

	@MockitoBean
	protected ClientIpResolver clientIpResolver;

	@MockitoBean
	protected SearchService searchService;

	@MockitoBean
	protected MainService mainService;

	@MockitoBean
	protected MemberService memberService;

	@MockitoBean
	protected FavoriteService favoriteService;

	@MockitoBean
	protected FileService fileService;

	@MockitoBean
	protected StorageProperties storageProperties;

	@MockitoBean
	protected NotificationService notificationService;

	@MockitoBean
	protected NotificationPreferenceService notificationPreferenceService;

	@MockitoBean
	protected PromotionService promotionService;

	@MockitoBean
	protected AnnouncementService announcementService;

	@MockitoBean
	protected MenuService menuService;

	@MockitoBean
	protected TokenRefreshService tokenRefreshService;

	@MockitoBean
	protected ChatService chatService;

	@MockitoBean
	protected ClientActivityIngestService clientActivityIngestService;

	@MockitoBean
	protected FoodCategoryService foodCategoryService;

	@MockitoBean
	protected GeocodeService geocodeService;

	@MockitoBean
	protected ReportService reportService;

	@MockitoBean
	protected HealthEndpoint healthEndpoint;

	@MockitoBean
	protected MemberRepository memberRepository;

	@MockitoBean
	protected RefreshTokenStore refreshTokenStore;
}
