package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.test.context.support.WithSecurityContext;

import com.tasteam.config.WithCustomMockUserSecurityContextFactory;
import com.tasteam.domain.member.entity.MemberRole;

/**
 * 통합/슬라이스 테스트에서 인증된 사용자 컨텍스트를 손쉽게 구성하기 위한 어노테이션
 * - id/password/role 값을 지정하면, {@link WithCustomMockUserSecurityContextFactory} 가
 *   해당 정보를 가진 CustomUserDetails로 SecurityContext를 채운다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCustomMockUserSecurityContextFactory.class)
public @interface WithCustomMockUser{
	long id() default 1L;

	String password() default "password";

	MemberRole role() default MemberRole.USER;
}
