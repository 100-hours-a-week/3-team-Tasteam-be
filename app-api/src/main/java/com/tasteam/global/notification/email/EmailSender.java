package com.tasteam.global.notification.email;

import java.util.Map;

public interface EmailSender {

	void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables);
}
