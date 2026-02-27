package com.tasteam.domain.chat.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private final ChatWebSocketAuthInterceptor chatWebSocketAuthInterceptor;
	private final ChatWebSocketHeartbeatProperties heartbeatProperties;
	@Qualifier("chatHeartbeatTaskScheduler")
	private final TaskScheduler heartbeatTaskScheduler;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws/chat").setAllowedOrigins("*");
	}

	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.setApplicationDestinationPrefixes("/pub");
		SimpleBrokerRegistration brokerRegistration = registry.enableSimpleBroker("/topic");
		if (heartbeatProperties.enabled()) {
			brokerRegistration
				.setHeartbeatValue(new long[] {
					heartbeatProperties.serverToClient(),
					heartbeatProperties.clientToServer()
				})
				.setTaskScheduler(heartbeatTaskScheduler);
		}
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(chatWebSocketAuthInterceptor);
	}

}
