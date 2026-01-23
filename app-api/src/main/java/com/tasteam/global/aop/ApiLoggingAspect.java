package com.tasteam.global.aop;

import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API 요청/응답 로깅을 담당하는 AOP 애스펙트
 * 모든 컨트롤러 메서드에 대해 요청 정보와 실행 시간을 로그로 기록
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "tasteam.aop.logging", name = {"enabled",
	"api-logging.enabled"}, havingValue = "true", matchIfMissing = false)
public class ApiLoggingAspect {

	private static final Logger log = LoggerFactory.getLogger("spring.aop.API");
	private static final AtomicLong REQUEST_ID_COUNTER = new AtomicLong(0);

	/**
	 * 컨트롤러 메서드 실행 전후로 API 호출 정보를 로깅
	 * 요청 ID, HTTP 메서드, URI, IP 주소, 실행 시간을 기록
	 */
	@Around("execution(* com.tasteam.domain.*.controller.*.*(..))")
	public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
		ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();

		if (attributes == null) {
			return joinPoint.proceed();
		}

		HttpServletRequest request = attributes.getRequest();
		String requestId = String.format("%03d", REQUEST_ID_COUNTER.incrementAndGet());
		long startTime = System.currentTimeMillis();

		log.info("ID={} | {} {} | IP={}",
			requestId,
			request.getMethod(),
			request.getRequestURI(),
			request.getRemoteAddr());

		Object result = joinPoint.proceed();
		long duration = System.currentTimeMillis() - startTime;

		log.info("ID={} | Time={}ms", requestId, duration);

		return result;
	}
}
