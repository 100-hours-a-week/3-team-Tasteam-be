package com.tasteam.config.fake;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import com.tasteam.global.security.jwt.common.JwtTokenConstants;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class FakeJwtTokenProvider {

	private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-must-be-long-enough";
	private final SecretKey secretKey;

	public FakeJwtTokenProvider() {
		this.secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
	}

	public String generateExpiredRefreshToken(Long memberId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() - 1000);

		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim(JwtTokenConstants.CLAIM_TYPE, JwtTokenConstants.TOKEN_TYPE_REFRESH)
			.issuedAt(new Date(now.getTime() - 2000))
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	public String generateInvalidRefreshToken(Long memberId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() - 1000);

		SecretKey invalidKey = Keys.hmacShaKeyFor(
			"invalid_jwt_key-for-jwt-token-generation-must-be-long-enough".getBytes(StandardCharsets.UTF_8));
		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim(JwtTokenConstants.CLAIM_TYPE, JwtTokenConstants.TOKEN_TYPE_REFRESH)
			.issuedAt(new Date(now.getTime() - 2000))
			.expiration(expiryDate)
			.signWith(invalidKey, Jwts.SIG.HS256)
			.compact();
	}

	public String generateExpiredAccessToken(Long memberId, String role) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() - 1000);

		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim(JwtTokenConstants.CLAIM_ROLE, role)
			.claim(JwtTokenConstants.CLAIM_TYPE, JwtTokenConstants.TOKEN_TYPE_ACCESS)
			.issuedAt(new Date(now.getTime() - 2000))
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}
}
