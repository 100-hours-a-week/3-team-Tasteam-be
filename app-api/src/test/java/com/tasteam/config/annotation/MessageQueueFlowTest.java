package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.tasteam.config.TestMessageQueueConfiguration;

/**
 * MQ 이벤트 라우팅 배선(wiring) 검증용 통합 테스트 어노테이션.
 *
 * <p>전체 애플리케이션 컨텍스트 대신 {@link TestMessageQueueConfiguration}만 로드하는
 * 최소 컨텍스트를 생성한다. MQ producer·consumer는 mock으로 등록되므로 Mockito verify()를
 * 사용해 발행/구독 흐름을 검증할 수 있다.
 *
 * <p>테스트별로 추가 빈이 필요하면 {@code @Import(LocalConfig.class)}로 확장한다.
 *
 * <pre>
 * {@code
 * @MessageQueueFlowTest
 * @Import(SomeFlowIntegrationTest.TestConfig.class)
 * class SomeFlowIntegrationTest {
 *     @Configuration
 *     static class TestConfig {
 *         // 테스트별 빈 (publisher, registrar, 도메인 서비스 mock 등)
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMessageQueueConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
public @interface MessageQueueFlowTest{}
