package com.tasteam.global.security.logout.handler;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tasteam.global.security.common.logout.LogoutAction;
import com.tasteam.global.security.common.util.SecurityResponseSender;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogoutHandler {

	private final SecurityResponseSender securityResponseSender;
	private final List<LogoutAction> logoutActions;

	public void onLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {

		SecurityContextHolder.clearContext();

		for (LogoutAction logoutAction : logoutActions) {
			logoutAction.onLogout(request, response);
		}

		securityResponseSender.sendSuccess(response, HttpServletResponse.SC_OK, "로그아웃되었습니다.");
	}
}
