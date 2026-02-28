package com.tasteam.global.notification.email;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnMissingBean(EmailSender.class)
public class LoggingEmailSender implements EmailSender {

	@Override
	public void sendGroupJoinVerification(String email, String code, Instant expiresAt) {
		log.info("Email verification code sent. email={}, code={}, expiresAt={}", email, code, expiresAt);
	}

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		log.info("[EMAIL] templateKey={}, to={}, vars={}", templateKey, toEmail, variables);
	}
}
