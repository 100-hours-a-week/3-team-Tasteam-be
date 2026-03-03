package com.tasteam.global.aop;

import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * @Transactional 메서드의 쿼리 실행 횟수와 실행 시간을 로깅하는 AOP 애스펙트
 * N+1 쿼리 문제 등 성능 이슈를 모니터링하는 데 유용
 */
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.aop.logging", name = {"enabled",
	"transactional-query-logging.enabled"}, havingValue = "true", matchIfMissing = false)
public class TransactionalQueryLoggingAspect {

	private static final Logger log = LoggerFactory.getLogger("spring.aop.Transaction");
	private static final AtomicLong TX_ID_COUNTER = new AtomicLong(0);
	private final EntityManager entityManager;

	/**
	 * 트랜잭션 메서드 실행 전후로 쿼리 실행 횟수와 시간을 로깅
	 * Hibernate Statistics를 사용하여 실제 실행된 쿼리 개수를 추적
	 */
	@Around("@annotation(org.springframework.transaction.annotation.Transactional)")
	public Object logTransactionQueryCount(ProceedingJoinPoint joinPoint) throws Throwable {
		String txId = String.format("%03d", TX_ID_COUNTER.incrementAndGet());
		Session session = entityManager.unwrap(Session.class);
		Statistics stats = session.getSessionFactory().getStatistics();
		// NOTE: SessionFactory-wide stats. 동시 요청 환경에서 카운트 오차 가능 (±N).
		//       정확한 개별 쿼리 분석은 pg_stat_statements 사용.
		long startQueryCount = stats.getPrepareStatementCount();
		long startTime = System.currentTimeMillis();

		try {
			Object result = joinPoint.proceed();
			long executedQueries = stats.getPrepareStatementCount() - startQueryCount;
			long elapsedTime = System.currentTimeMillis() - startTime;

			log.info("ID={} | Method={} | Queries={} | Time={}ms",
				txId, joinPoint.getSignature().getName(), executedQueries, elapsedTime);
			return result;
		} catch (Throwable throwable) {
			long executedQueries = stats.getPrepareStatementCount() - startQueryCount;
			long elapsedTime = System.currentTimeMillis() - startTime;

			log.error("ID={} | Method={} | Queries={} | Time={}ms | Error={}",
				txId, joinPoint.getSignature().getName(), executedQueries, elapsedTime, throwable.getMessage());
			throw throwable;
		}
	}
}
