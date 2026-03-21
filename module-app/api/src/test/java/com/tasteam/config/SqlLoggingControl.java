package com.tasteam.config;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * 테스트에서 SQL 로깅을 동적으로 제어하는 유틸리티 클래스
 * 특정 코드 블록에서만 SQL 로그를 활성화할 수 있습니다.
 */
public class SqlLoggingControl {

	private static final Logger SQL_LOGGER = (Logger)LoggerFactory.getLogger("org.hibernate.SQL");
	private static final Logger BINDER_LOGGER = (Logger)LoggerFactory
		.getLogger("org.hibernate.type.descriptor.sql.BasicBinder");

	/**
	 * SQL 로깅을 활성화합니다.
	 */
	public static void enable() {
		SQL_LOGGER.setLevel(Level.DEBUG);
		BINDER_LOGGER.setLevel(Level.TRACE);
	}

	/**
	 * SQL 로깅을 비활성화합니다.
	 */
	public static void disable() {
		SQL_LOGGER.setLevel(Level.INFO);
		BINDER_LOGGER.setLevel(Level.INFO);
	}

	/**
	 * 특정 코드 블록에서만 SQL 로깅을 활성화합니다.
	 *
	 * @param runnable 실행할 코드 블록
	 */
	public static void withLogging(Runnable runnable) {
		enable();
		try {
			runnable.run();
		} finally {
			disable();
		}
	}

	/**
	 * 특정 코드 블록에서만 SQL 로깅을 활성화하고 결과를 반환합니다.
	 *
	 * @param supplier 실행할 코드 블록
	 * @param <T> 반환 타입
	 * @return 코드 블록의 실행 결과
	 */
	public static <T> T withLogging(java.util.function.Supplier<T> supplier) {
		enable();
		try {
			return supplier.get();
		} finally {
			disable();
		}
	}
}
