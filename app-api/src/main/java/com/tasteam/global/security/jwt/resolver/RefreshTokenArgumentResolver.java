package com.tasteam.global.security.jwt.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.security.jwt.annotation.RefreshToken;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 리프레시 토큰을 컨트롤러 메서드 인자로 주입하는 Argument Resolver
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenArgumentResolver implements HandlerMethodArgumentResolver {

	private final JwtCookieProvider jwtCookieProvider;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RefreshToken.class)
			&& parameter.getParameterType().equals(String.class);
	}

	/** 
	 * 요청에서 리프레시 토큰을 추출하여 반환
	 */
	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
		}

		return jwtCookieProvider.getRefreshTokenFromCookie(request)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
	}
}
