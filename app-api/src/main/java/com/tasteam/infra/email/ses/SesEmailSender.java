package com.tasteam.infra.email.ses;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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

	@Override
	public void sendGroupJoinVerification(String email, String code, Instant expiresAt) {
		String subject = "[Tasteam] 그룹 참여 인증 코드";
		String htmlBody = buildVerificationHtml(code, expiresAt);
		send(email, subject, htmlBody);
	}

	@Override
	public void sendTemplateEmail(String toEmail, String templateKey, Map<String, Object> variables) {
		String subject = "[Tasteam] " + templateKey;
		String htmlBody = buildGenericHtml(templateKey, variables);
		send(toEmail, subject, htmlBody);
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

	private String buildVerificationHtml(String code, Instant expiresAt) {
		String expiresAtKst = KST_FORMATTER.format(expiresAt);
		return """
			<html>
			<body style="font-family:sans-serif;padding:24px;max-width:480px;margin:auto;">
			  <h2 style="color:#E24C4B;">Tasteam 그룹 참여 인증</h2>
			  <p>아래 인증 코드를 입력하세요.</p>
			  <div style="font-size:36px;font-weight:bold;letter-spacing:10px;
			              background:#f5f5f5;padding:16px;text-align:center;border-radius:8px;">
			    %s
			  </div>
			  <p style="color:#888;font-size:13px;margin-top:12px;">만료 시각: %s (KST)</p>
			  <p style="color:#aaa;font-size:11px;">본 메일은 발신 전용입니다.</p>
			</body>
			</html>
			""".formatted(code, expiresAtKst);
	}

	private String buildGenericHtml(String templateKey, Map<String, Object> variables) {
		StringBuilder rows = new StringBuilder();
		variables.forEach((k, v) -> rows.append("<tr>")
			.append("<td style='padding:4px 8px;color:#555;font-weight:bold;'>").append(k).append("</td>")
			.append("<td style='padding:4px 8px;'>").append(v).append("</td>")
			.append("</tr>"));
		return """
			<html>
			<body style="font-family:sans-serif;padding:24px;max-width:480px;margin:auto;">
			  <h2 style="color:#E24C4B;">Tasteam</h2>
			  <p>%s</p>
			  <table style="border-collapse:collapse;width:100%%;">%s</table>
			  <p style="color:#aaa;font-size:11px;margin-top:16px;">본 메일은 발신 전용입니다.</p>
			</body>
			</html>
			""".formatted(templateKey, rows);
	}
}
