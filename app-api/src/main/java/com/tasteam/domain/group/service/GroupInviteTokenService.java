package com.tasteam.domain.group.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.security.jwt.properties.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class GroupInviteTokenService {

	private static final String CLAIM_GROUP_ID = "groupId";
	private static final String CLAIM_TYPE = "type";
	private static final String INVITE_TYPE = "GROUP_INVITE";
	private static final Duration INVITE_TTL = Duration.ofMinutes(5);

	private final SecretKey secretKey;

	public GroupInviteTokenService(JwtProperties jwtProperties) {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}

	public InviteToken issue(Long groupId, String email) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(INVITE_TTL);
		String token = Jwts.builder()
			.subject(email)
			.claim(CLAIM_GROUP_ID, groupId)
			.claim(CLAIM_TYPE, INVITE_TYPE)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
		return new InviteToken(token, expiresAt);
	}

	public InviteClaims parse(String token) {
		if (!StringUtils.hasText(token)) {
			throw new BusinessException(GroupErrorCode.EMAIL_TOKEN_INVALID);
		}
		try {
			Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();

			String type = claims.get(CLAIM_TYPE, String.class);
			String email = claims.getSubject();
			Number groupIdRaw = claims.get(CLAIM_GROUP_ID, Number.class);

			if (!INVITE_TYPE.equals(type) || !StringUtils.hasText(email) || groupIdRaw == null) {
				throw new BusinessException(GroupErrorCode.EMAIL_TOKEN_INVALID);
			}
			return new InviteClaims(email, groupIdRaw.longValue());
		} catch (ExpiredJwtException ex) {
			throw new BusinessException(GroupErrorCode.EMAIL_TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new BusinessException(GroupErrorCode.EMAIL_TOKEN_INVALID);
		}
	}

	public record InviteToken(
		String token,
		Instant expiresAt) {
	}

	public record InviteClaims(
		String email,
		Long groupId) {
	}
}
