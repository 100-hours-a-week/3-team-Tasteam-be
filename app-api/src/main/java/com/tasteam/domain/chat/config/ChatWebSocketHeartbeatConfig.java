package com.tasteam.domain.chat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties(ChatWebSocketHeartbeatProperties.class)
public class ChatWebSocketHeartbeatConfig {

	@Bean(name = "chatHeartbeatTaskScheduler")
	public ThreadPoolTaskScheduler chatHeartbeatTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("chat-heartbeat-");
		scheduler.setDaemon(true);
		scheduler.initialize();
		return scheduler;
	}
}
