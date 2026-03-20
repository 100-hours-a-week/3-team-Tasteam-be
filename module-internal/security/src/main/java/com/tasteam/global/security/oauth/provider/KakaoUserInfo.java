package com.tasteam.global.security.oauth.provider;

import java.util.Map;

public class KakaoUserInfo implements OAuthUserInfo {
	private final Map<String, Object> attributes;

	public KakaoUserInfo(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String getProvider() {
		return "kakao";
	}

	@Override
	public String getId() {
		Object id = attributes.get("id");
		return id == null ? null : String.valueOf(id);
	}

	@Override
	public String getEmail() {
		Object accountObj = attributes.get("kakao_account");
		if (!(accountObj instanceof Map<?, ?> account)) {
			return null;
		}
		Object email = account.get("email");
		return email == null ? null : String.valueOf(email);
	}

	@Override
	public String getName() {
		Object propertiesObj = attributes.get("properties");
		if (propertiesObj instanceof Map<?, ?> properties) {
			Object nickname = properties.get("nickname");
			if (nickname != null) {
				return String.valueOf(nickname);
			}
		}
		Object accountObj = attributes.get("kakao_account");
		if (accountObj instanceof Map<?, ?> account) {
			Object profileObj = account.get("profile");
			if (profileObj instanceof Map<?, ?> profile) {
				Object nickname = profile.get("nickname");
				if (nickname != null) {
					return String.valueOf(nickname);
				}
			}
		}
		return null;
	}
}
