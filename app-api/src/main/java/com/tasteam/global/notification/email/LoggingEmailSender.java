package com.tasteam.global.notification.email;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {

	@Override
	public void sendGroupJoinVerificationLink(String email, String verificationUrl, Instant expiresAt) {
		log.info("Group invite verification link sent. email={}, url={}, expiresAt={}",
			email,
			redactToken(verificationUrl), // 그룹 가입 인증 토큰 노출 방지
			expiresAt);
	}

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		log.info("[EMAIL] templateKey={}, to={}, vars={}", templateKey, toEmail, variables);
	}

	private String redactToken(String url) {
		if (url == null) {
			return null;
		}
		return url.replaceAll("([?&]token=)[^&]+", "$1[REDACTED]");
	}
}
