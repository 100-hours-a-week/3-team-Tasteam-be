package com.tasteam.infra.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.webhook.event.ErrorContext;
import com.tasteam.infra.webhook.event.ErrorOccurredEvent;
import com.tasteam.infra.webhook.template.BusinessExceptionTemplate;
import com.tasteam.infra.webhook.template.SystemExceptionTemplate;

@UnitTest
@DisplayName("WebhookErrorEventListener")
class WebhookErrorEventListenerTest {

	@Mock
	private WebhookClient webhookClient;

	@Mock
	private BusinessExceptionTemplate businessExceptionTemplate;

	@Mock
	private SystemExceptionTemplate systemExceptionTemplate;

	private WebhookProperties webhookProperties;
	private WebhookErrorEventListener listener;

	@BeforeEach
	void setUp() {
		webhookProperties = new WebhookProperties();
		listener = new WebhookErrorEventListener(
			webhookClient, businessExceptionTemplate, systemExceptionTemplate, webhookProperties);
	}

	@Test
	@DisplayName("웹훅 클라이언트가 비활성화되어 있으면 전송하지 않는다")
	void onErrorOccurred_whenClientDisabled_doesNotSend() {
		given(webhookClient.isEnabled()).willReturn(false);
		ErrorOccurredEvent event = errorEvent("SYSTEM", "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

		listener.onErrorOccurred(event);

		then(webhookClient).should(never()).send(any());
	}

	@Test
	@DisplayName("제외 에러코드에 해당하면 전송하지 않는다")
	void onErrorOccurred_whenExcludedErrorCode_doesNotSend() {
		given(webhookClient.isEnabled()).willReturn(true);
		webhookProperties.getFilters().setExcludeErrorCodes(List.of("NOT_FOUND"));
		ErrorOccurredEvent event = errorEvent("BUSINESS", "NOT_FOUND", HttpStatus.NOT_FOUND);

		listener.onErrorOccurred(event);

		then(webhookClient).should(never()).send(any());
	}

	@Test
	@DisplayName("minHttpStatus 미만이면 전송하지 않는다")
	void onErrorOccurred_whenBelowMinHttpStatus_doesNotSend() {
		given(webhookClient.isEnabled()).willReturn(true);
		webhookProperties.getFilters().setMinHttpStatus(500);
		ErrorOccurredEvent event = errorEvent("BUSINESS", "BAD_REQUEST", HttpStatus.BAD_REQUEST);

		listener.onErrorOccurred(event);

		then(webhookClient).should(never()).send(any());
	}

	@Test
	@DisplayName("BUSINESS 에러 타입이면 businessExceptionTemplate을 사용한다")
	void onErrorOccurred_whenBusinessType_usesBusinessTemplate() {
		given(webhookClient.isEnabled()).willReturn(true);
		webhookProperties.getFilters().setMinHttpStatus(400);
		ErrorOccurredEvent event = errorEvent("BUSINESS", "SOME_ERROR", HttpStatus.BAD_REQUEST);
		WebhookMessage message = new WebhookMessage("title", "desc", null, null, Instant.now(), null);
		given(businessExceptionTemplate.build(event.context())).willReturn(message);

		listener.onErrorOccurred(event);

		then(businessExceptionTemplate).should().build(event.context());
		then(webhookClient).should().send(message);
	}

	@Test
	@DisplayName("SYSTEM 에러 타입이면 systemExceptionTemplate을 사용한다")
	void onErrorOccurred_whenSystemType_usesSystemTemplate() {
		given(webhookClient.isEnabled()).willReturn(true);
		webhookProperties.getFilters().setMinHttpStatus(500);
		ErrorOccurredEvent event = errorEvent("SYSTEM", "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
		WebhookMessage message = new WebhookMessage("title", "desc", null, null, Instant.now(), null);
		given(systemExceptionTemplate.build(event.context())).willReturn(message);

		listener.onErrorOccurred(event);

		then(systemExceptionTemplate).should().build(event.context());
		then(webhookClient).should().send(message);
	}

	@Test
	@DisplayName("전송 중 예외가 발생해도 전파되지 않는다")
	void onErrorOccurred_whenSendThrows_doesNotPropagate() {
		given(webhookClient.isEnabled()).willReturn(true);
		webhookProperties.getFilters().setMinHttpStatus(500);
		ErrorOccurredEvent event = errorEvent("SYSTEM", "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
		WebhookMessage message = new WebhookMessage("title", "desc", null, null, Instant.now(), null);
		given(systemExceptionTemplate.build(event.context())).willReturn(message);
		willThrow(new RuntimeException("전송 실패")).given(webhookClient).send(message);

		assertThatCode(() -> listener.onErrorOccurred(event))
			.doesNotThrowAnyException();
	}

	private ErrorOccurredEvent errorEvent(String errorType, String errorCode, HttpStatus httpStatus) {
		ErrorContext context = new ErrorContext(
			errorType, errorCode, "테스트 에러 메시지", httpStatus,
			"GET", "/api/test", "JUnit", Instant.now(), "RuntimeException", null);
		return new ErrorOccurredEvent(context);
	}
}
