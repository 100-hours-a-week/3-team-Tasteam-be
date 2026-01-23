package com.tasteam.global.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import com.tasteam.global.exception.business.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 컨트롤러에서 발생하는 예외를 로깅하는 AOP 애스펙트
 * BusinessException과 시스템 예외를 구분하여 처리하며,
 * 프로덕션 환경에서는 스택 트레이스를 제외하여 로그 크기를 최적화
 */
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aop.exception-logging.enabled", havingValue = "true", matchIfMissing = true)
public class ExceptionLoggingAspect {

	private static final Logger log = LoggerFactory.getLogger("spring.aop.Exception");
	private final Environment environment;

	/**
	 * 컨트롤러 메서드에서 예외 발생 시 로깅
	 * BusinessException은 WARN 레벨, 시스템 예외는 ERROR 레벨로 기록
	 */
	@AfterThrowing(pointcut = "execution(* com.tasteam.domain.*.controller.*.*(..))", throwing = "ex")
	public void logException(JoinPoint joinPoint, Throwable ex) {
		String method = joinPoint.getSignature().toShortString();
		boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));

		if (ex instanceof BusinessException businessException) {
			logBusinessException(method, businessException, isProd);
		} else {
			logSystemException(method, ex, isProd);
		}
	}

	/**
	 * 비즈니스 예외 로깅 (WARN 레벨)
	 * 개발 환경에서는 스택 트레이스 포함, 프로덕션에서는 제외
	 */
	private void logBusinessException(String method, BusinessException ex, boolean isProd) {
		if (isProd) {
			log.warn("BusinessException at {} | Code={} | Message={}",
				method, ex.getErrorCode(), ex.getMessage());
		} else {
			log.warn("BusinessException at {} | Code={} | Message={}",
				method, ex.getErrorCode(), ex.getMessage(), ex);
		}
	}

	/**
	 * 시스템 예외 로깅 (ERROR 레벨)
	 * 개발 환경에서는 스택 트레이스 포함, 프로덕션에서는 제외
	 */
	private void logSystemException(String method, Throwable ex, boolean isProd) {
		if (isProd) {
			log.error("Exception at {} | Message={}", method, ex.getMessage());
		} else {
			log.error("Exception at {} | Message={}", method, ex.getMessage(), ex);
		}
	}
}
