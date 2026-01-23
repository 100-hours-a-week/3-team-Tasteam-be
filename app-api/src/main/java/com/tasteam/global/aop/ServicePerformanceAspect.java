package com.tasteam.global.aop;

import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 서비스 레이어 메서드의 실행 시간을 측정하고 로깅하는 AOP 애스펙트
 * 1초 이상 소요되는 메서드는 WARN 레벨로 별도 표시
 */
@Aspect
@Component
@ConditionalOnProperty(name = "aop.service-performance.enabled", havingValue = "true", matchIfMissing = true)
public class ServicePerformanceAspect {

	private static final Logger log = LoggerFactory.getLogger("spring.aop.Service");
	private static final AtomicLong EXEC_ID_COUNTER = new AtomicLong(0);

	/**
	 * 서비스 메서드 실행 시간 측정 및 로깅
	 * 1000ms 초과 시 SLOW 상태로 WARN 레벨 로깅
	 */
	@Around("execution(* com.tasteam.domain.*.service.*.*(..))")
	public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
		String execId = String.format("%03d", EXEC_ID_COUNTER.incrementAndGet());
		String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
		String methodName = joinPoint.getSignature().getName();
		long startTime = System.currentTimeMillis();

		try {
			Object result = joinPoint.proceed();
			long executionTime = System.currentTimeMillis() - startTime;

			if (executionTime > 1000) {
				log.warn("ID={} | Method={}.{} | Time={}ms | Status=SLOW",
					execId, className, methodName, executionTime);
			} else {
				log.info("ID={} | Method={}.{} | Time={}ms",
					execId, className, methodName, executionTime);
			}

			return result;
		} catch (Throwable throwable) {
			long executionTime = System.currentTimeMillis() - startTime;
			log.error("ID={} | Method={}.{} | Time={}ms | Error={}",
				execId, className, methodName, executionTime, throwable.getMessage());
			throw throwable;
		}
	}
}
