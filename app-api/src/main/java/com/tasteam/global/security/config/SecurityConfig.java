package com.tasteam.global.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfigurationSource;

import com.tasteam.global.security.common.constants.SecurityConstants;
import com.tasteam.global.security.exception.filter.FilterChainExceptionFilter;
import com.tasteam.global.security.exception.handler.CustomAccessDeniedHandler;
import com.tasteam.global.security.exception.handler.CustomAuthenticationEntryPoint;
import com.tasteam.global.security.jwt.config.JwtSecurityConfig;
import com.tasteam.global.security.oauth.config.OAuth2SecurityConfig;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CorsConfigurationSource corsConfigurationSource;
	private final CustomAuthenticationEntryPoint authenticationEntryPoint;
	private final CustomAccessDeniedHandler accessDeniedHandler;
	private final FilterChainExceptionFilter filterChainExceptionFilter;
	private final JwtSecurityConfig jwtSecurityConfig;
	private final OAuth2SecurityConfig oAuth2SecurityConfig;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			/// [CORS 설정]
			.cors(cors -> cors.configurationSource(corsConfigurationSource))

			/// [CSRF 설정] : 비활성화 (Why?: REST API + JWT stateless이므로 불필요) */
			.csrf(AbstractHttpConfigurer::disable)

			/// [Session 캐싱 설정] : 비활성화 (Why?: REST API + JWT stateless JWT access header / refresh cookie)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			/// [Request 캐시 설정] = 비황성화 (Wht?: REST API + JWT stateless이므로 불필요)
			.requestCache(cache -> cache
				.requestCache(new NullRequestCache()))

			/// [Request 권한 설정]
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll()
				.requestMatchers(SecurityConstants.SECURE_URLS).hasRole("USER")
				.requestMatchers(SecurityConstants.ADMIN_URLS).hasRole("ADMIN")
				.anyRequest().authenticated())

			.formLogin(AbstractHttpConfigurer::disable)

			/// [필터 체인 전역 예외 헨들러] : 모든 예외
			.addFilterBefore(
				filterChainExceptionFilter,
				UsernamePasswordAuthenticationFilter.class)

			/// [Auth 예외 처리 핸들러 설정] : (401, 403)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))

		;

		jwtSecurityConfig.configure(http);
		oAuth2SecurityConfig.configure(http);

		return http.build();
	}
}
