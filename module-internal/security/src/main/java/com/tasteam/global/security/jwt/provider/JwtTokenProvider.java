package com.tasteam.global.security.jwt.provider;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.tasteam.global.security.jwt.common.JwtTokenConstants;
import com.tasteam.global.security.jwt.properties.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰 제공자
 */
@Slf4j
@Component
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;
	private final SecretKey secretKey;

	public JwtTokenProvider(JwtProperties properties) {
		this.jwtProperties = properties;
		this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)); // HS256 비밀 키 생성
	}

	public String generateAccessToken(Long memberId, String role) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim(JwtTokenConstants.CLAIM_ROLE, role)
			.claim(JwtTokenConstants.CLAIM_TYPE, JwtTokenConstants.TOKEN_TYPE_ACCESS)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	public String generateAccessToken(Long memberId, String role, long expirationMs) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim(JwtTokenConstants.CLAIM_ROLE, role)
			.claim(JwtTokenConstants.CLAIM_TYPE, JwtTokenConstants.TOKEN_TYPE_ACCESS)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	public String generateRefreshToken(Long memberId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.id(UUID.randomUUID().toString())
			.claim(JwtTokenConstants.CLAIM_TYPE, JwtTokenConstants.TOKEN_TYPE_REFRESH)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	public Claims validateToken(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public Long getUidFromToken(String token) {
		Claims claims = validateToken(token);
		return Long.parseLong(claims.getSubject());
	}

	public String getRoleFromToken(String token) {
		Claims claims = validateToken(token);
		return claims.get(JwtTokenConstants.CLAIM_ROLE, String.class);
	}

	public String getTokenType(String token) {
		Claims claims = validateToken(token);
		return claims.get(JwtTokenConstants.CLAIM_TYPE, String.class);
	}

	public Date getExpiration(String token) {
		Claims claims = validateToken(token);
		return claims.getExpiration();
	}

	public long getExpiresIn(String token) {
		Date expiration = validateToken(token).getExpiration();
		return expiration.getTime() - System.currentTimeMillis();
	}

	public boolean isAccessToken(String token) {
		return JwtTokenConstants.TOKEN_TYPE_ACCESS.equals(getTokenType(token));
	}

	public boolean isRefreshToken(String token) {
		return JwtTokenConstants.TOKEN_TYPE_REFRESH.equals(getTokenType(token));
	}

	public boolean isTokenExpired(String token) {
		try {
			Claims claims = validateToken(token);
			return claims.getExpiration().before(new Date());
		} catch (Exception e) {
			return true;
		}
	}
}
