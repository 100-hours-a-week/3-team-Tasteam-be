package com.tasteam.global.security.common.constants;

public class SecurityConstants {

	public static final String LOGIN_URL = "/api/v1/auth/login";
	public static final String LOGOUT_URL = "/api/v1/auth/logout";
	public static final String SIGNUP_URL = "/api/v1/auth/signup";
	public static final String REFRESH_TOKEN_URL = "/api/v1/auth/token/refresh";
	public static final String AUTH_COOKIE_PATH = "/api/v1/auth";
	public static final String OAUTH_URL = "/oauth2/**";
	public static final String OAUTH_API_URL = "/api/v1/auth/oauth/**";
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";

	public static final String HEALTH_CHECK_URL = "/api/v1/health";

	public static final String[] PUBLIC_URLS = {
		LOGIN_URL, LOGOUT_URL, SIGNUP_URL, REFRESH_TOKEN_URL,
		HEALTH_CHECK_URL,
		OAUTH_URL, OAUTH_API_URL,
		"/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui/**",
		"/swagger-resources/**", "/webjars/**", "/actuator/**",
		"/api/v1/restaurants/*/reviews", "/api/v1/restaurants/*",
		"/api/v1/reviews/*",
		"/groups", "/groups/**",
		"/api/v1/files/**",
		"/api/v1/test/**"
	};

	public static final String[] SECURE_URLS = {
		"/api/*/secure/**", "/api/*/user/**"
	};

	public static final String[] ADMIN_URLS = {
		"/api/*/admin/**", "/admin/**"
	};
}
