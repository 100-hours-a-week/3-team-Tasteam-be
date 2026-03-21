package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.tasteam.api.ApiApplication;
import com.tasteam.config.TestStorageConfiguration;
import com.tasteam.config.TestcontainersConfiguration;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({TestStorageConfiguration.class, TestcontainersConfiguration.class})
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("perf")
public @interface PerformanceTest{}
