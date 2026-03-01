package com.tasteam.infra.email.log;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.tasteam.infra.email.EmailSender;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "tasteam.notification.email", name = "provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		log.info("[EMAIL] templateKey={}, to={}, vars={}", templateKey, toEmail, variables);
	}
}
