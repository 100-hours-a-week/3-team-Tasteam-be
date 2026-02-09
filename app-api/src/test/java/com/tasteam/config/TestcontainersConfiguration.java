package com.tasteam.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:15-3.3")
			.asCompatibleSubstituteFor("postgres"))
			// 테스트 속도를 위해 컨테이너 재사용은 유지하되,
			// max_connections를 넉넉히 올려 다중 컨텍스트 실행 시 커넥션 부족을 방지한다.
			.withReuse(true)
			.withCommand("postgres", "-c", "max_connections=200");
	}
}
