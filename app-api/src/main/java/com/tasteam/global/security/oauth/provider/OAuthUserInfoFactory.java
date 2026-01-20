package com.tasteam.global.security.oauth.provider;

import java.util.Map;

/**
 * OAuth 사용자 정보 팩토리
 */
public class OAuthUserInfoFactory {
	public static OAuthUserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
		return switch (registrationId.toLowerCase()) {
			case "google" -> new GoogleUserInfo(attributes);
			case "kakao" -> new KakaoUserInfo(attributes);
			case "github" -> new GithubUserInfo(attributes);
			default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 Provider: " + registrationId);
		};
	}
}
