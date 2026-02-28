package com.tasteam.infra.email.ses;

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
import com.amazonaws.services.simpleemail.model.MessageRejectedException;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.tasteam.global.notification.email.EmailNotificationProperties;
import com.tasteam.global.notification.email.EmailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "tasteam.notification.email", name = "provider", havingValue = "ses")
@RequiredArgsConstructor
public class SesEmailSender implements EmailSender {

	private final AmazonSimpleEmailService sesClient;
	private final EmailNotificationProperties emailProperties;
	private final TemplateEngine templateEngine;

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		Context ctx = new Context();
		ctx.setVariables(variables);
		String html = templateEngine.process("email/" + templateKey, ctx);
		String subject = extractSubject(variables);
		send(toEmail, subject, null, html);
	}

	private void send(String to, String subject, String textBody, String htmlBody) {
		String charset = emailProperties.getSes().getCharset();
		Body body = new Body();
		if (textBody != null) {
			body.withText(new Content().withCharset(charset).withData(textBody));
		}
		if (htmlBody != null) {
			body.withHtml(new Content().withCharset(charset).withData(htmlBody));
		}
		SendEmailRequest request = new SendEmailRequest()
			.withSource(emailProperties.getSes().getFromAddress())
			.withDestination(new Destination().withToAddresses(to))
			.withMessage(new Message()
				.withSubject(new Content().withCharset(charset).withData(subject))
				.withBody(body));
		try {
			sesClient.sendEmail(request);
			log.info("[SES] 이메일 발송 완료. to={}", to);
		} catch (MessageRejectedException e) {
			log.error("[SES] 메일 거부됨. to={}, reason={}. "
				+ "샌드박스 모드인 경우 수신자 이메일을 SES Verified Identity로 등록해야 합니다.",
				to, e.getMessage());
			throw new RuntimeException("이메일 발송이 거부되었습니다. to=" + to, e);
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
