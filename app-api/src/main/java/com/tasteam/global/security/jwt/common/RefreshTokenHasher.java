package com.tasteam.global.security.jwt.common;

import org.apache.commons.codec.digest.DigestUtils;

public final class RefreshTokenHasher {

	private RefreshTokenHasher() {}

	public static String hash(String token) {
		return DigestUtils.sha256Hex(token);
	}
}
