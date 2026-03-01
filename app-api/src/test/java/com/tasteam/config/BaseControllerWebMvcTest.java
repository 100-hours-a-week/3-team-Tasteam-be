package com.tasteam.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.announcement.controller.AnnouncementController;
import com.tasteam.domain.announcement.service.AnnouncementService;
import com.tasteam.domain.favorite.service.FavoriteService;
import com.tasteam.domain.file.controller.FileController;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.controller.GroupController;
import com.tasteam.domain.group.service.GroupFacade;
import com.tasteam.domain.main.controller.MainController;
import com.tasteam.domain.main.service.MainService;
import com.tasteam.domain.member.controller.MemberController;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.notification.controller.NotificationController;
import com.tasteam.domain.notification.service.NotificationPreferenceService;
import com.tasteam.domain.notification.service.NotificationService;
import com.tasteam.domain.promotion.controller.PromotionController;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.domain.restaurant.controller.MenuController;
import com.tasteam.domain.restaurant.controller.RestaurantController;
import com.tasteam.domain.restaurant.service.MenuService;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.controller.ReviewController;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.search.controller.RecentSearchController;
import com.tasteam.domain.search.controller.SearchController;
import com.tasteam.domain.search.service.SearchService;
import com.tasteam.domain.subgroup.controller.SubgroupController;
import com.tasteam.domain.subgroup.service.SubgroupFacade;
import com.tasteam.global.ratelimit.ClientIpResolver;
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
	AnnouncementController.class
})
public abstract class BaseControllerWebMvcTest {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

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
}
