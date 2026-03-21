package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Message) KafkaConnectConnectorRegistrar 단위 테스트")
class KafkaConnectConnectorRegistrarTest {

	@Test
	@DisplayName("커넥터 튜닝 설정을 Kafka Connect 요청 본문에 반영한다")
	void buildConnectorConfig_appliesConfiguredTuningValues() {
		// given
		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		properties.getUserActivityS3Ingest().setTopic("evt.user-activity.s3-ingest.v1");
		properties.getUserActivityS3Ingest().setDlqTopic("evt.user-activity.s3-ingest.v1.dlq");
		properties.getConnector().getUserActivityS3Sink().setBucket("tasteam-analytics-local");
		properties.getConnector().getUserActivityS3Sink().setRegion("ap-northeast-2");
		properties.getConnector().getUserActivityS3Sink().setTasksMax(1);
		properties.getConnector().getUserActivityS3Sink().setFlushSize(100);
		properties.getConnector().getUserActivityS3Sink().setRotateIntervalMs(60000L);
		properties.getConnector().getUserActivityS3Sink().setRotateScheduleIntervalMs(60000L);
		properties.getConnector().getUserActivityS3Sink().setEndpoint("http://minio:9000");
		properties.getConnector().getUserActivityS3Sink().setPathStyleAccess(true);
		properties.getConnector().getUserActivityS3Sink().setAccessKeyId("minioadmin");
		properties.getConnector().getUserActivityS3Sink().setSecretAccessKey("minioadmin");
		KafkaConnectConnectorRegistrar registrar = new KafkaConnectConnectorRegistrar(properties);

		// when
		@SuppressWarnings("unchecked") Map<String, Object> config = (Map<String, Object>)ReflectionTestUtils
			.invokeMethod(
				registrar,
				"buildConnectorConfig");

		// then
		assertThat(config)
			.containsEntry("tasks.max", "1")
			.containsEntry("flush.size", "100")
			.containsEntry("rotate.interval.ms", "60000")
			.containsEntry("rotate.schedule.interval.ms", "60000")
			.containsEntry("s3.bucket.name", "tasteam-analytics-local")
			.containsEntry("s3.endpoint", "http://minio:9000")
			.containsEntry("s3.path.style.access", "true")
			.containsEntry("errors.deadletterqueue.topic.name", "evt.user-activity.s3-ingest.v1.dlq");
	}
}
