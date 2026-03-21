package com.tasteam.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.tasteam.config.annotation.WithCustomMockUser;
import com.tasteam.global.security.user.dto.CustomUserDetails;

/**
 * {@link WithCustomMockUser} 어노테이션을 사용했을 때
 * 테스트 SecurityContext에 주입할 인증 정보를 생성하는 팩토리
 * - 테스트에서 지정한 id/password/role 값을 기반으로 CustomUserDetails를 만들고
 * - UsernamePasswordAuthenticationToken 으로 감싸 SecurityContextHolder에 설정한다.
 */
public class WithCustomMockUserSecurityContextFactory implements WithSecurityContextFactory<WithCustomMockUser> {

	@Override
	public SecurityContext createSecurityContext(WithCustomMockUser annotation) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

		CustomUserDetails userDetails = new CustomUserDetails(
			annotation.id(),
			annotation.password(),
			annotation.role().name());

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
			userDetails.getAuthorities());

		context.setAuthentication(authentication);
		return context;
	}
}
