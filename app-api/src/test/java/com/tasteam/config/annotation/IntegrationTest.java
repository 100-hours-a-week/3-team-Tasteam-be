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

import com.tasteam.config.JpaAuditingTestConfig;
import com.tasteam.config.TestSecurityConfig;
import com.tasteam.config.TestStorageConfiguration;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, JpaAuditingTestConfig.class, TestStorageConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
public @interface IntegrationTest{}
