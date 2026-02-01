package com.tasteam.global.security.jwt.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.security.common.constants.SecurityConstants;
import com.tasteam.global.security.exception.model.CustomAuthenticationException;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.user.dto.CustomUserDetails;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 필터 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	/** 
	 * 요청에서 JWT를 추출하고 유효성을 검사한 후, 인증 정보를 SecurityContext에 설정합니다.
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		try {
			String jwt = extractJwtFromRequest(request);

			if (jwt != null && jwtTokenProvider.isAccessToken(jwt) && !jwtTokenProvider.isTokenExpired(jwt)) {
				Long uid = jwtTokenProvider.getUidFromToken(jwt);
				String role = jwtTokenProvider.getRoleFromToken(jwt);

				UserDetails userDetails = new CustomUserDetails(uid, null, role);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (ExpiredJwtException e) {
			log.debug("만료된 JWT 토큰: {}", e.getMessage());
			throw new CustomAuthenticationException(AuthErrorCode.AUTHENTICATION_REQUIRED, "토큰이 만료되었습니다.");
		} catch (JwtException e) {
			log.debug("유효하지 않은 JWT 토큰: {}", e.getMessage());
			throw new CustomAuthenticationException(AuthErrorCode.AUTHENTICATION_REQUIRED, "유효하지 않은 토큰입니다.");
		} catch (Exception e) {
			log.error("JWT 인증 처리 중 오류: {}", e.getMessage());
			throw new CustomAuthenticationException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증 처리 중 오류가 발생했습니다.");
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * 요청에서 JWT 토큰을 추출합니다.
	 */
	private String extractJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.BEARER_PREFIX)) {
			return bearerToken.substring(SecurityConstants.BEARER_PREFIX.length());
		}

		return null;
	}
}
