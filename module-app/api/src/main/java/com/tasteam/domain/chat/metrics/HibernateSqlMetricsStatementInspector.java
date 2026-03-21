package com.tasteam.domain.chat.metrics;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HibernateSqlMetricsStatementInspector implements StatementInspector, HibernatePropertiesCustomizer {

	private final ChatSendDbQueryTracker chatSendDbQueryTracker;

	@Override
	public String inspect(String sql) {
		chatSendDbQueryTracker.recordSql(sql);
		return sql;
	}

	@Override
	public void customize(Map<String, Object> hibernateProperties) {
		hibernateProperties.put(AvailableSettings.STATEMENT_INSPECTOR, this);
	}
}
