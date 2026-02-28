package com.tasteam.global.security.common.constants;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import java.util.List;

import org.springframework.http.HttpMethod;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 보안 정책 정의 - 어떤 엔드포인트를 누가 접근할 수 있는지
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiEndpointSecurityPolicy {

	/**
	 * 인증 없이 접근 가능한 엔드포인트
	 */
	public static List<EndpointPermission> publicEndpoints() {
		return List.of(

			// 인증
			permit(POST, ApiEndpoints.LOGIN),
			permit(POST, ApiEndpoints.SIGNUP),
			permit(POST, ApiEndpoints.LOGOUT),
			permit(POST, ApiEndpoints.AUTH_TOKEN),
			permit(POST, ApiEndpoints.AUTH_TOKEN_TEST),
			permit(POST, ApiEndpoints.REFRESH_TOKEN),

			permit(GET, ApiEndpoints.AUTH_OAUTH_ALL),
			permit(GET, ApiEndpoints.AUTH_OAUTH_CALLBACK),
			permit(GET, ApiEndpoints.OAUTH2_ALL),

			// 음식점 조회
			permit(GET, ApiEndpoints.RESTAURANTS),
			permit(GET, ApiEndpoints.RESTAURANTS_DETAIL),
			permit(GET, ApiEndpoints.RESTAURANTS_REVIEWS),
			permit(GET, ApiEndpoints.RESTAURANTS_MENUS),
			permit(GET, ApiEndpoints.FOOD_CATEGORIES),

			// 메인
			permit(GET, ApiEndpoints.MAIN),
			permit(GET, ApiEndpoints.MAIN_ALL),

			// 프로모션 조회
			permit(GET, ApiEndpoints.PROMOTIONS),
			permit(GET, ApiEndpoints.PROMOTIONS_DETAIL),

			// 공지사항 조회
			permit(GET, ApiEndpoints.ANNOUNCEMENTS),
			permit(GET, ApiEndpoints.ANNOUNCEMENTS_DETAIL),

			// 리뷰 조회
			permit(GET, ApiEndpoints.REVIEWS),
			permit(GET, ApiEndpoints.REVIEWS_DETAIL),
			permit(GET, ApiEndpoints.GROUPS_REVIEWS),
			permit(GET, ApiEndpoints.GROUPS_REVIEWS_RESTAURANTS),
			permit(GET, ApiEndpoints.SUBGROUPS_REVIEWS),
			permit(GET, ApiEndpoints.MEMBERS_REVIEWS),
			permit(GET, ApiEndpoints.GROUPS_DETAIL),
			permit(GET, ApiEndpoints.GROUPS_SUBGROUPS),
			permit(GET, ApiEndpoints.GROUPS_SUBGROUPS_SEARCH),

			// 역지오코딩 (토큰 유무 무관)
			permit(GET, ApiEndpoints.GEOCODE_REVERSE),

			// 검색 (토큰 유무 무관)
			permit(POST, ApiEndpoints.SEARCH),

			// Analytics Ingest (토큰 유무 무관)
			permit(POST, ApiEndpoints.ANALYTICS_EVENTS),

			// Swagger
			permit(GET, ApiEndpoints.SWAGGER_UI),
			permit(GET, ApiEndpoints.SWAGGER_UI_INDEX),
			permit(GET, ApiEndpoints.SWAGGER_RESOURCES),
			permit(GET, ApiEndpoints.SWAGGER_UI_ASSETS),
			permit(GET, ApiEndpoints.API_DOCS),
			permit(GET, ApiEndpoints.WEBJARS),

			// 모니터링
			permit(GET, ApiEndpoints.ACTUATOR),
			permit(GET, ApiEndpoints.HEALTH_CHECK),

			// Admin Static Pages
			permit(GET, ApiEndpoints.ADMIN_STATIC),
			permit(POST, ApiEndpoints.ADMIN_AUTH_LOGIN),

			// Test
			permit(GET, ApiEndpoints.TEST),

			// WebSocket
			permit(GET, ApiEndpoints.WEBSOCKET));
	}

	/**
	 * USER 권한 필요
	 */
	public static String[] userEndpoints() {
		return new String[] {
			ApiEndpoints.FILES_ALL,
			ApiEndpoints.GROUPS_ALL,
			ApiEndpoints.SUBGROUPS_ALL,
			ApiEndpoints.CHAT_ROOMS_ALL,
			ApiEndpoints.MEMBERS_ALL,
			ApiEndpoints.REPORTS_ALL
		};
	}

	/**
	 * ADMIN 권한 필요
	 */
	public static String[] adminEndpoints() {
		return new String[] {
			ApiEndpoints.ADMIN_ALL
		};
	}

	private static EndpointPermission permit(HttpMethod method, String pattern) {
		return new EndpointPermission(method, pattern);
	}

	public record EndpointPermission(HttpMethod method, String pattern) {
	}
}
