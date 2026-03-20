package com.tasteam.global.security.oauth.dto;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuthUserDetails implements OAuth2User {

	private final Long uid;
	private final String role;

	public CustomOAuthUserDetails(Long uid, String role) {
		this.uid = uid;
		this.role = role;
	}

	public Long getUid() {
		return uid;
	}

	@Override
	public Map<String, Object> getAttributes() {
		Map<String, Object> auth = new HashMap<>();

		auth.put("username", uid);
		auth.put("uid", uid);
		auth.put("role", role);

		return auth;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public String getName() {
		return uid.toString();
	}

}
