package com.tasteam.global.notification.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.mail")
public class MailProperties {

	private boolean enabled;
	private String from;
	private String fromName;
	private String appUrl;
}
