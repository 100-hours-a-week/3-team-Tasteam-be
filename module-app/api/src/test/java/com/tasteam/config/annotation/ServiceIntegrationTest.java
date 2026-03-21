package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.tasteam.config.TestStorageConfiguration;
import com.tasteam.config.TestcontainersConfiguration;

/**
 * 서비스/도메인 레이어 통합 테스트 전용 메타 어노테이션입니다.
 * 웹 MVC 계층은 띄우지 않고 경량 컨텍스트로 로드해 빠른 서비스 수준 테스트를 수행합니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({TestStorageConfiguration.class, TestcontainersConfiguration.class})
@SpringBootTest(classes = ServiceIntegrationTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("service-integration")
public @interface ServiceIntegrationTest{}
