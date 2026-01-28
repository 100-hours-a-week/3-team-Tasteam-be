package com.tasteam.global.swagger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

/**
 * Swagger에 Exception Response Description을 설정하기 위한 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomErrorResponseDescription{

	Class<? extends SwaggerErrorResponseDescription> value();

	String group();
}
