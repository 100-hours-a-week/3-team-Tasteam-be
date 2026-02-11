package com.tasteam.global.security.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * API 엔드포인트 URL 상수 정의
 * 보안 정책과 무관하게 순수하게 URL만 관리
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiEndpoints {

	// Postfix
	private static final String DETAIL = "/*";
	private static final String ALL = "/**";

	// API Version Prefix
	private static final String API_V1 = "/api/v1";

	// Auth
	public static final String AUTH = API_V1 + "/auth";
	public static final String LOGIN = AUTH + "/login";
	public static final String LOGOUT = AUTH + "/logout";
	public static final String SIGNUP = AUTH + "/signup";
	public static final String AUTH_TOKEN = AUTH + "/token";
	public static final String AUTH_TOKEN_TEST = AUTH + "/token/test";
	public static final String REFRESH_TOKEN = AUTH + "/token/refresh";
	public static final String AUTH_OAUTH = AUTH + "/oauth";
	public static final String AUTH_OAUTH_ALL = AUTH_OAUTH + ALL;
	public static final String AUTH_OAUTH_CALLBACK = AUTH + "/oauth/callback";
	public static final String OAUTH2_ALL = "/oauth2" + ALL;

	// Health
	public static final String HEALTH_CHECK = API_V1 + "/health";

	// Swagger
	public static final String API_DOCS = "/api/v1/v3/api-docs" + ALL;
	public static final String SWAGGER_UI = "/api/v1/swagger-ui.html";
	public static final String SWAGGER_UI_INDEX = "/api/v1/swagger-ui/index.html";
	public static final String SWAGGER_UI_ASSETS = "/api/v1/swagger-ui" + ALL;
	public static final String SWAGGER_RESOURCES = "/api/v1/swagger-resources" + ALL;

	public static final String WEBJARS = "/webjars" + ALL;
	public static final String ACTUATOR = "/actuator" + ALL;
	public static final String ACTUATOR_HEALTH = "/actuator/health";

	// Static Admin Pages
	public static final String ADMIN_STATIC = "/admin" + ALL;
	public static final String ADMIN_AUTH = API_V1 + "/admin/auth";
	public static final String ADMIN_AUTH_LOGIN = ADMIN_AUTH + "/login";

	// Member
	public static final String MEMBERS = API_V1 + "/members";
	public static final String MEMBERS_ALL = MEMBERS + ALL;
	public static final String MEMBERS_DETAIL = MEMBERS + DETAIL;
	public static final String MEMBERS_REVIEWS = MEMBERS_DETAIL + "/reviews";

	// Restaurant
	public static final String RESTAURANTS = API_V1 + "/restaurants";
	public static final String RESTAURANTS_DETAIL = RESTAURANTS + DETAIL;
	public static final String RESTAURANTS_REVIEWS = RESTAURANTS_DETAIL + "/reviews";
	public static final String RESTAURANTS_MENUS = RESTAURANTS_DETAIL + "/menus";
	public static final String FOOD_CATEGORIES = API_V1 + "/food-categories";

	// Main
	public static final String MAIN = API_V1 + "/main";

	// Promotion
	public static final String PROMOTIONS = API_V1 + "/promotions";
	public static final String PROMOTIONS_DETAIL = PROMOTIONS + DETAIL;

	// Review
	public static final String REVIEWS = API_V1 + "/reviews";
	public static final String REVIEWS_DETAIL = REVIEWS + DETAIL;

	// Group
	public static final String GROUPS = API_V1 + "/groups";
	public static final String GROUPS_ALL = GROUPS + ALL;
	public static final String GROUPS_DETAIL = GROUPS + DETAIL;
	public static final String GROUPS_REVIEWS = GROUPS_DETAIL + "/reviews";
	public static final String GROUPS_REVIEWS_RESTAURANTS = GROUPS_REVIEWS + "/restaurants";
	public static final String GROUPS_SUBGROUPS = GROUPS + "/*/subgroups";
	public static final String GROUPS_SUBGROUPS_SEARCH = GROUPS + "/*/subgroups/search";

	// Subgroup
	public static final String SUBGROUPS = API_V1 + "/subgroups";
	public static final String SUBGROUPS_ALL = SUBGROUPS + ALL;
	public static final String SUBGROUPS_DETAIL = SUBGROUPS + DETAIL;
	public static final String SUBGROUPS_REVIEWS = SUBGROUPS_DETAIL + "/reviews";

	// Search
	public static final String SEARCH = API_V1 + "/search";

	// File
	public static final String FILES = API_V1 + "/files";
	public static final String FILES_ALL = FILES + ALL;

	// Test
	public static final String TEST = API_V1 + "/test" + ALL;

	// Admin
	public static final String ADMIN = API_V1 + "/admin";
	public static final String ADMIN_ALL = ADMIN + ALL;
}
