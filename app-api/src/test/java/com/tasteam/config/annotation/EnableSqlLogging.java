package com.tasteam.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * 테스트 메서드에서 SQL 로깅을 활성화하는 어노테이션
 * 테스트 실행 중에만 SQL 로그가 출력되고, @Sql 스크립트 실행 시에는 로그가 억제된다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnableSqlLogging.SqlLoggingExtension.class)
public @interface EnableSqlLogging{

	class SqlLoggingExtension implements BeforeEachCallback, AfterEachCallback {

		private Level previousSqlLevel;
		private Level previousBinderLevel;

		@Override
		public void beforeEach(ExtensionContext context) {
			Logger sqlLogger = (Logger)LoggerFactory.getLogger("org.hibernate.SQL");
			Logger binderLogger = (Logger)LoggerFactory.getLogger("org.hibernate.type.descriptor.sql.BasicBinder");

			// 현재 레벨 저장
			previousSqlLevel = sqlLogger.getLevel();
			previousBinderLevel = binderLogger.getLevel();

			// DEBUG 레벨로 변경
			sqlLogger.setLevel(Level.DEBUG);
			binderLogger.setLevel(Level.TRACE);
		}

		@Override
		public void afterEach(ExtensionContext context) {
			Logger sqlLogger = (Logger)LoggerFactory.getLogger("org.hibernate.SQL");
			Logger binderLogger = (Logger)LoggerFactory.getLogger("org.hibernate.type.descriptor.sql.BasicBinder");

			// 원래 레벨로 복구
			sqlLogger.setLevel(previousSqlLevel);
			binderLogger.setLevel(previousBinderLevel);
		}
	}
}
