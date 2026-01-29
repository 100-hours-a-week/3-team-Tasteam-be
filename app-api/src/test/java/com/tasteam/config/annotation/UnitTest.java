package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 순수 단위 테스트를 위한 메타 어노테이션입니다.
 * - @ExtendWith(MockitoExtension.class) : Mockito 확장을 활성화하여 @Mock/@InjectMocks 사용을 지원
 * - @Tag(\"unit\") : Gradle/JUnit에서 단위 테스트만 필터링해 실행할 수 있도록 태그 부여
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public @interface UnitTest{}
