package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.tasteam.api.ApiApplication;
import com.tasteam.config.TestSecurityConfig;
import com.tasteam.config.TestStorageConfiguration;
import com.tasteam.config.TestcontainersConfiguration;

/**
 * API/웹 계층까지 포함한 통합 테스트 전용 메타 어노테이션입니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestStorageConfiguration.class, TestcontainersConfiguration.class})
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Tag("integration")
public @interface IntegrationTest{}
