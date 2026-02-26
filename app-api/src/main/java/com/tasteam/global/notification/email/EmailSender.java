package com.tasteam.global.notification.email;

import java.time.Instant;
import java.util.Map;

public interface EmailSender {

	void sendGroupJoinVerification(String email, String code, Instant expiresAt);

	void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables);
}
