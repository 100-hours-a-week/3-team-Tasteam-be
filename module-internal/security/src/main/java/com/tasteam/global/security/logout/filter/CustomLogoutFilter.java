package com.tasteam.global.security.logout.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tasteam.global.security.common.constants.ApiEndpoints;
import com.tasteam.global.security.logout.handler.LogoutHandler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomLogoutFilter extends OncePerRequestFilter {

	private final LogoutHandler logoutHandler;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String requestURI = request.getRequestURI();
		String method = request.getMethod();

		if (!ApiEndpoints.LOGOUT.equals(requestURI) || !"POST".equals(method)) {
			filterChain.doFilter(request, response);
			return;
		}

		logoutHandler.onLogout(request, response);

	}
}
