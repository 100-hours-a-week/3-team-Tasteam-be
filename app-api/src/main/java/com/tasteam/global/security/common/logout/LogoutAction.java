package com.tasteam.global.security.common.logout;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 로그아웃 시 수행할 확장 가능한 작업(SPI).
 * <p>
 * 예: refresh token 블랙리스트 등록, 쿠키 삭제, 외부 세션 정리 등.
 */
public interface LogoutAction {

	void onLogout(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
