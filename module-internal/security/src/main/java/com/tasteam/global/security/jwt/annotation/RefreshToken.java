package com.tasteam.global.security.jwt.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * 리프레시 토큰을 메서드 인자로 주입하기 위한 커스텀 애노테이션
 */
@Documented
@Parameter(hidden = true)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RefreshToken{}
