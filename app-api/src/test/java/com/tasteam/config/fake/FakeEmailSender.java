package com.tasteam.config.fake;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.tasteam.global.notification.email.EmailSender;

@Component
@Profile("test")
public class FakeEmailSender implements EmailSender {

	private final List<SentEmail> sentEmails = new CopyOnWriteArrayList<>();

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		sentEmails.add(new SentEmail(toEmail, templateKey, Map.copyOf(variables)));
	}

	public List<SentEmail> getSentEmails() {
		return Collections.unmodifiableList(sentEmails);
	}

	public boolean hasEmailSentTo(String toEmail) {
		return sentEmails.stream().anyMatch(e -> e.toEmail().equals(toEmail));
	}

	public boolean hasEmailSentWith(String templateKey) {
		return sentEmails.stream().anyMatch(e -> e.templateKey().equals(templateKey));
	}

	public void clear() {
		sentEmails.clear();
	}

	public record SentEmail(String toEmail, String templateKey, Map<String, Object> variables) {
	}
}
