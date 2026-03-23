package com.tasteam.global.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](AOP) AsyncPipelineMetricsAspect 단위 테스트")
class AsyncPipelineMetricsAspectTest {

	@Test
	@DisplayName("@ObservedAsyncPipeline 성공/실패를 각각 카운팅하고 지연 시간을 기록한다")
	void observeAsyncPipeline_recordsMetrics() throws Throwable {
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		AsyncPipelineMetricsAspect aspect = new AsyncPipelineMetricsAspect(meterRegistry);
		ObservedAsyncPipeline annotation = annotation("successPipeline", ObservedAsyncPipeline.class);
		ProceedingJoinPoint successJoinPoint = mock(ProceedingJoinPoint.class);
		ProceedingJoinPoint failedJoinPoint = mock(ProceedingJoinPoint.class);
		when(successJoinPoint.proceed()).thenReturn(null);
		when(failedJoinPoint.proceed()).thenThrow(new IllegalStateException("테스트 예외"));

		aspect.observeAsyncPipeline(successJoinPoint, annotation);
		assertThatThrownBy(() -> aspect.observeAsyncPipeline(failedJoinPoint, annotation))
			.isInstanceOf(IllegalStateException.class);

		Counter successCounter = meterRegistry.find("async.pipeline.notification.outbox_scan.process")
			.tag("result", "success")
			.counter();
		Counter failCounter = meterRegistry.find("async.pipeline.notification.outbox_scan.process")
			.tag("result", "fail")
			.counter();
		Timer successTimer = meterRegistry.find("async.pipeline.notification.outbox_scan.latency")
			.tag("result", "success")
			.timer();
		Timer failTimer = meterRegistry.find("async.pipeline.notification.outbox_scan.latency")
			.tag("result", "fail")
			.timer();
		String meterSnapshot = meterRegistry.getMeters()
			.stream()
			.map(meter -> meter.getId().toString())
			.collect(Collectors.joining(", "));

		assertThat(successCounter).withFailMessage("meters=%s", meterSnapshot).isNotNull();
		assertThat(failCounter).withFailMessage("meters=%s", meterSnapshot).isNotNull();
		assertThat(successTimer).withFailMessage("meters=%s", meterSnapshot).isNotNull();
		assertThat(failTimer).withFailMessage("meters=%s", meterSnapshot).isNotNull();
		assertThat(successCounter.count()).isEqualTo(1.0);
		assertThat(failCounter.count()).isEqualTo(1.0);
		assertThat(successTimer.count()).isEqualTo(1L);
		assertThat(failTimer.count()).isEqualTo(1L);
	}

	@Test
	@DisplayName("@ObservedOutbox 메서드 호출 시 snapshot 메트릭을 기록한다")
	void observeOutboxSnapshot_recordsMetrics() throws Throwable {
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		AsyncPipelineMetricsAspect aspect = new AsyncPipelineMetricsAspect(meterRegistry);
		ObservedOutbox annotation = annotation("snapshot", ObservedOutbox.class);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.proceed()).thenReturn(null);
		aspect.observeOutboxSnapshot(joinPoint, annotation);

		Counter snapshotCounter = meterRegistry.find("outbox.notification.snapshot")
			.tag("result", "success")
			.counter();
		Timer snapshotTimer = meterRegistry.find("outbox.notification.snapshot.latency")
			.tag("result", "success")
			.timer();

		assertThat(snapshotCounter).isNotNull();
		assertThat(snapshotTimer).isNotNull();
		assertThat(snapshotCounter.count()).isEqualTo(1.0);
		assertThat(snapshotTimer.count()).isEqualTo(1L);
	}

	private <T extends java.lang.annotation.Annotation> T annotation(String methodName, Class<T> annotationType) {
		try {
			Method method = ObservedTarget.class.getMethod(methodName);
			return method.getAnnotation(annotationType);
		} catch (Exception ex) {
			throw new IllegalStateException("테스트 어노테이션 조회에 실패했습니다.", ex);
		}
	}

	static class ObservedTarget {

		@ObservedAsyncPipeline(domain = "notification", stage = "outbox_scan")
		public void successPipeline() {}

		@ObservedAsyncPipeline(domain = "notification", stage = "outbox_scan")
		public void failedPipeline() {
			throw new IllegalStateException("테스트 예외");
		}

		@ObservedOutbox(name = "notification")
		public void snapshot() {}
	}
}
