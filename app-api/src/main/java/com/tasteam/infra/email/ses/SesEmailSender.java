package com.tasteam.infra.email.ses;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.tasteam.global.notification.email.EmailSender;
import com.tasteam.infra.email.EmailProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "tasteam.email", name = "type", havingValue = "ses")
@RequiredArgsConstructor
public class SesEmailSender implements EmailSender {

	private static final DateTimeFormatter KST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		.withZone(ZoneId.of("Asia/Seoul"));

	private final AmazonSimpleEmailService sesClient;
	private final EmailProperties emailProperties;
	private final TemplateEngine templateEngine;

	@Override
	public void sendGroupJoinVerification(String email, String code, Instant expiresAt) {
		Context ctx = new Context();
		ctx.setVariable("code", code);
		ctx.setVariable("expiresAt", KST_FORMATTER.format(expiresAt));
		String html = templateEngine.process("email/group-join-verification", ctx);
		send(email, "[Tasteam] 그룹 참여 인증 코드", html);
	}

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		Context ctx = new Context();
		ctx.setVariables(variables);
		String html = templateEngine.process("email/" + templateKey, ctx);
		String subject = extractSubject(variables);
		send(toEmail, subject, html);
	}

	private void send(String to, String subject, String htmlBody) {
		SendEmailRequest request = new SendEmailRequest()
			.withSource(emailProperties.getFrom())
			.withDestination(new Destination().withToAddresses(to))
			.withMessage(new Message()
				.withSubject(new Content().withCharset("UTF-8").withData(subject))
				.withBody(new Body()
					.withHtml(new Content().withCharset("UTF-8").withData(htmlBody))));
		try {
			sesClient.sendEmail(request);
			log.info("[SES] 이메일 발송 완료. to={}", to);
		} catch (Exception e) {
			log.error("[SES] 이메일 발송 실패. to={}", to, e);
			throw new RuntimeException("이메일 발송에 실패했습니다. to=" + to, e);
		}
	}

	private String extractSubject(Map<String, Object> variables) {
		Object s = variables.get("subject");
		if (s instanceof String str && !str.isBlank()) {
			return str;
		}
		Object t = variables.get("title");
		return t instanceof String ts ? ts : "";
	}
}
