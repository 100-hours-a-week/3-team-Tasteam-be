package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import com.tasteam.config.TestSecurityConfig;

/**
 * WebMvc 슬라이스 테스트에서 반복되는 보안 관련 설정을 묶어둔 메타 어노테이션
 * - @WebMvcTest : 지정한 컨트롤러만 로드
 * - @AutoConfigureMockMvc(addFilters = false) : 스프링 시큐리티 필터 비활성화
 * - @Import(TestSecurityConfig.class) : 테스트용 모의 보안 빈 주입
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public @interface ControllerWebMvcTest{

	@AliasFor(annotation = WebMvcTest.class, attribute = "value")
	Class<?>[] value() default {};

	@AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
	Class<?>[] controllers() default {};
}
