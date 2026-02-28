package com.tasteam.global.notification.email;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "tasteam.notification.email", name = "provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

	@Override
	public void sendGroupJoinVerificationLink(String email, String verificationUrl, Instant expiresAt) {
		log.info("Group invite verification link sent. email={}, expiresAt={}",
			email,
			expiresAt);
	}

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		log.info("[EMAIL] templateKey={}, to={}, vars={}", templateKey, toEmail, variables);
	}
}
