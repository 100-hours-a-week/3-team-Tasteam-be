package com.tasteam.global.security.exception.filter;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tasteam.global.security.common.util.SecurityResponseSender;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 필터 체인 내에서 발생하는 예외를 처리하는 필터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FilterChainExceptionFilter extends OncePerRequestFilter {

	private final SecurityResponseSender securityResponseSender;

	/**
	 * 사용자 입력을 로그에 넣기 전에 개행 문자를 제거해 로그 인젝션을 방지한다.
	 */
	private String sanitizeForLog(String value) {
		if (value == null) {
			return null;
		}
		return value.replace('\r', ' ').replace('\n', ' ');
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (AuthenticationException | AccessDeniedException e) {
			throw e;
		} catch (Exception e) {
			String requestId = sanitizeForLog(request.getHeader("X-Request-Id"));
			String method = sanitizeForLog(request.getMethod());
			String uri = sanitizeForLog(request.getRequestURI());
			log.error("FilterChain exception. requestId={}, method={}, uri={}", requestId, method, uri, e);
			securityResponseSender.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"서버 오류가 발생했습니다");
		}
	}
}
