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
 * RestaurantAnalysisFacade 실행 시간을 측정해 로깅하는 AOP 애스펙트.
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "tasteam.aop.logging", name = {"enabled",
	"ai-analysis-performance.enabled"}, havingValue = "true", matchIfMissing = false)
public class AiAnalysisPerformanceAspect {

	private static final Logger log = LoggerFactory.getLogger("spring.aop.AIAnalysis");
	private static final AtomicLong EXEC_ID_COUNTER = new AtomicLong(0);

	@Around("execution(* com.tasteam.domain.restaurant.service.analysis.RestaurantAnalysisFacade.*(..))")
	public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
		String execId = String.format("%03d", EXEC_ID_COUNTER.incrementAndGet());
		String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
		String methodName = joinPoint.getSignature().getName();
		long startTime = System.currentTimeMillis();

		try {
			Object result = joinPoint.proceed();
			long executionTime = System.currentTimeMillis() - startTime;
			log.info("ID={} | Method={}.{} | Time={}ms",
				execId, className, methodName, executionTime);
			return result;
		} catch (Throwable throwable) {
			long executionTime = System.currentTimeMillis() - startTime;
			log.error("ID={} | Method={}.{} | Time={}ms | Error={}",
				execId, className, methodName, executionTime, throwable.getMessage());
			throw throwable;
		}
	}
}
