package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * 배치 잡/메시지 리스너 등 잡성 플로우 통합 테스트를 위한 메타 어노테이션입니다.
 * - @ServiceIntegrationTest 를 기반으로 하며, job 전용 태그를 추가로 부여합니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ServiceIntegrationTest
@Tag("job-integration")
public @interface JobIntegrationTest{}
