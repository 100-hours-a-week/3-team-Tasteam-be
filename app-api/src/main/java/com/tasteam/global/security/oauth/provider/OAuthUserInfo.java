package com.tasteam.global.security.oauth.provider;

public interface OAuthUserInfo {
	String getId();

	String getProvider();

	String getEmail();

	String getName();
}