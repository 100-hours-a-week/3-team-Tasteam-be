package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

/**
 * MQ 이벤트 라우팅 배선(wiring) 검증용 통합 테스트 어노테이션.
 *
 * <p>최소 컨텍스트를 생성한다. MQ producer·consumer는 mock으로 등록되므로 Mockito verify()를
 * 사용해 발행/구독 흐름을 검증할 수 있다.
 *
 * <p>테스트 클래스에 {@code @SpringBootTest(classes = TestConfig.class, webEnvironment = NONE)}를
 * 함께 선언하고, TestConfig에 필요한 모든 빈(producer, consumer, properties, objectMapper 포함)을
 * 정의한다.
 *
 * <pre>
 * {@code
 * @MessageQueueFlowTest
 * @SpringBootTest(classes = SomeFlowIntegrationTest.TestConfig.class,
 *     webEnvironment = SpringBootTest.WebEnvironment.NONE)
 * class SomeFlowIntegrationTest {
 *     @Configuration
 *     static class TestConfig {
 *         // 공유 빈: messageQueueProperties, objectMapper, producer mock, consumer mock
 *         // 도메인 빈: publisher, registrar, 도메인 서비스 mock 등
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Tag("integration")
public @interface MessageQueueFlowTest{}
