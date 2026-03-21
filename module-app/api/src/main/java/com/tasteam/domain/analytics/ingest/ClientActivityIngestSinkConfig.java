package com.tasteam.domain.analytics.ingest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;
import com.tasteam.infra.messagequeue.UserActivityS3SinkPublisher;

@Configuration
public class ClientActivityIngestSinkConfig {

	@Bean
	@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "route", havingValue = "db", matchIfMissing = true)
	public ClientActivityIngestSink clientActivityDbSink(UserActivityEventStoreService userActivityEventStoreService) {
		return new ClientActivityDbSink(userActivityEventStoreService);
	}

	@Bean
	@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "route", havingValue = "s3")
	@ConditionalOnExpression("'${tasteam.message-queue.enabled:false}'.equals('true') && '${tasteam.message-queue.provider:none}'.equals('kafka')")
	public ClientActivityIngestSink clientActivityS3Sink(UserActivityS3SinkPublisher userActivityS3SinkPublisher) {
		return new ClientActivityS3Sink(userActivityS3SinkPublisher);
	}
}
