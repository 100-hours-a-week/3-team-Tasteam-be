package com.tasteam.global.swagger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Swagger UI에서 태그(API 그룹)의 표시 순서를 지정하는 어노테이션.
 * 값이 낮을수록 먼저 표시된다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerTagOrder{

	int value();
}
