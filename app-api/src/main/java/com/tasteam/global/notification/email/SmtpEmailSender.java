package com.tasteam.global.notification.email;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.mail", name = "enabled", havingValue = "true")
public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final MailProperties mailProperties;

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		try {
			Context ctx = new Context();
			ctx.setVariables(variables);
			String html = templateEngine.process("email/" + templateKey, ctx);

			String subject = extractSubject(variables);
			send(toEmail, subject, html);
		} catch (Exception ex) {
			log.error("템플릿 이메일 발송 실패. templateKey={}, to={}", templateKey, toEmail, ex);
			throw new RuntimeException("이메일 발송 실패", ex);
		}
	}

	@Override
	public void sendGroupJoinVerification(String email, String code, Instant expiresAt) {
		try {
			Context ctx = new Context();
			ctx.setVariable("code", code);
			ctx.setVariable("expiresAt", expiresAt);
			String html = templateEngine.process("email/group-join-verification", ctx);

			send(email, "[Tasteam] 그룹 가입 인증 코드", html);
		} catch (Exception ex) {
			log.error("그룹 가입 인증 이메일 발송 실패. email={}", email, ex);
			throw new RuntimeException("이메일 발송 실패", ex);
		}
	}

	private void send(String toEmail, String subject, String htmlBody) throws Exception {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
		helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
		helper.setTo(toEmail);
		helper.setSubject(subject);
		helper.setText(htmlBody, true);
		mailSender.send(message);
		log.info("[EMAIL] 발송 완료. to={}, subject={}", toEmail, subject);
	}

	private String extractSubject(Map<String, Object> variables) {
		Object subject = variables.get("subject");
		if (subject instanceof String s && !s.isBlank()) {
			return s;
		}
		Object title = variables.get("title");
		if (title instanceof String t && !t.isBlank()) {
			return t;
		}
		return "";
	}
}
