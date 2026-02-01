package com.tasteam.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.tasteam.config.annotation.ServiceIntegrationTest;

@ServiceIntegrationTest
class JpaAuditingConflictTest {

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("JPA Auditing 빈 중복 없이 컨텍스트가 정상 로드된다")
	void contextLoads() {
		assertThat(context).isNotNull();
	}

	@Test
	@DisplayName("jpaAuditingHandler 빈이 하나만 등록된다")
	void jpaAuditingHandlerExists() {
		assertThat(context.containsBean("jpaAuditingHandler")).isTrue();
		String[] beanNames = context.getBeanNamesForType(
			org.springframework.data.auditing.AuditingHandler.class);
		assertThat(beanNames).hasSize(1);
	}
}
