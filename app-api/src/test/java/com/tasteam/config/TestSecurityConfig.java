package com.tasteam.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.jwt.resolver.CurrentUserArgumentResolver;
import com.tasteam.global.security.logout.handler.LogoutHandler;

/**
 * 테스트 환경에서만 적용되는 보안 설정
 * 실제 보안 필터나 인증 과정을 로드하지 않고도 테스트를 수행할 수 있도록,
 * 사용자 정보와 관련된 컴포넌트들을 테스트 전용으로 대체한다.
 */
@TestConfiguration
public class TestSecurityConfig {

	/**
	 * 테스트용 인증 사용자 정보를 관리하는 컨텍스트를 생성한다.
	 * -> 테스트 코드에서 임의로 사용자 ID를 설정하거나 조회할 수 있다.
	 */
	@Bean
	public TestCurrentUserContext testCurrentUserContext() {
		return new TestCurrentUserContext();
	}

	/**
	 * 테스트에서 사용할 CurrentUserArgumentResolver 구현을 제공한다.
	 * -> 실제 인증 로직 대신 TestCurrentUserContext에 저장된 사용자 ID를 반환하도록 한다.
	 */
	@Bean
	@Primary
	public CurrentUserArgumentResolver testCurrentUserArgumentResolver(TestCurrentUserContext context) {
		return new CurrentUserArgumentResolver() {
			@Override
			public Object resolveArgument(org.springframework.core.MethodParameter parameter,
				org.springframework.web.method.support.ModelAndViewContainer mavContainer,
				org.springframework.web.context.request.NativeWebRequest webRequest,
				org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
				return context.getCurrentUserId();
			}
		};
	}

	/**
	 * 테스트 환경에서 사용할 Mock LogoutHandler를 생성한다.
	 * -> 실제 로그아웃 로직을 차단하고 Mockito 기반 Mock 객체를 제공한다.
	 */
	@Bean
	@ConditionalOnMissingBean
	public LogoutHandler logoutHandler() {
		return Mockito.mock(LogoutHandler.class);
	}

	/**
	 * Jwt 인증 필터의 의존성을 모두 Mock으로 제공한다.
	 * -> JwtTokenProvider와 LoginService를 빈으로 등록해 실제 구현체를 로드하지 않아도 된다.
	 */
	@Bean
	@ConditionalOnMissingBean
	@Primary
	public JwtTokenProvider jwtTokenProvider() {
		return Mockito.mock(JwtTokenProvider.class);
	}

}
