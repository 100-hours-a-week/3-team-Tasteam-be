package com.tasteam.global.security.jwt.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.security.user.dto.CustomUserDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * 현재 인증된 사용자의 UID를 컨트롤러 메서드 인자로 주입하는 Argument Resolver
 */
@Slf4j
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType().equals(Long.class);
	}

	/** 
	 * 현재 인증된 사용자의 UID를 반환
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
			return null;
		}

		CustomUserDetails user = (CustomUserDetails)auth.getPrincipal();
		return user.getUid();
	}
}
