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

/**
 * 필터 체인 내에서 발생하는 예외를 처리하는 필터
 */
@Component
@RequiredArgsConstructor
public class FilterChainExceptionFilter extends OncePerRequestFilter {

	private final SecurityResponseSender securityResponseSender;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (AuthenticationException | AccessDeniedException e) {
			throw e;
		} catch (Exception e) {
			securityResponseSender.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"서버 오류가 발생했습니다");
		}
	}
}
