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

	private static final DateTimeFormatter EXPIRES_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
		.withZone(ZoneId.of("Asia/Seoul"));

	private final AmazonSimpleEmailService sesClient;
	private final EmailNotificationProperties emailProperties;
	private final TemplateEngine templateEngine;

	@Override
	public void sendGroupJoinVerificationLink(String email, String verificationUrl, Instant expiresAt) {
		String subject = "[Tasteam] 그룹 가입 이메일 인증";
		String formattedExpiresAt = EXPIRES_AT_FORMATTER.format(expiresAt);
		String escapedVerificationUrl = escapeHtmlAttribute(verificationUrl);
		String textBody = """
			그룹 가입 인증을 완료하려면 아래 링크를 클릭해 주세요.

			%s

			만료 시각: %s
			""".formatted(verificationUrl, formattedExpiresAt);
		String htmlBody = """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			  <meta charset="UTF-8">
			  <meta name="viewport" content="width=device-width, initial-scale=1.0">
			</head>
			<body style="margin:0;padding:0;background-color:#f9fafb;font-family:'Apple SD Gothic Neo','Noto Sans KR',sans-serif;">
			  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb;padding:40px 0;">
			    <tr>
			      <td align="center">
			        <table width="480" cellpadding="0" cellspacing="0"
			               style="background-color:#ffffff;border-radius:16px;padding:48px 40px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
			          <tr>
			            <td style="padding-bottom:32px;">
			              <span style="font-size:22px;font-weight:700;color:#FFAE42;letter-spacing:-0.5px;">Tasteam</span>
			            </td>
			          </tr>
			          <tr>
			            <td style="padding-bottom:24px;">
			              <div style="font-size:64px;">✉️</div>
			            </td>
			          </tr>
			          <tr>
			            <td style="padding-bottom:12px;">
			              <h1 style="margin:0;font-size:24px;font-weight:700;color:#111827;">
			                이메일 <span style="color:#FFAE42;">인증</span>
			              </h1>
			            </td>
			          </tr>
			          <tr>
			            <td style="padding-bottom:32px;">
			              <p style="margin:0;font-size:15px;color:#6b7280;line-height:1.6;">
			                아래 버튼을 클릭하여<br>그룹 가입 인증을 완료해 주세요.
			              </p>
			            </td>
			          </tr>
			          <tr>
			            <td style="padding-bottom:32px;">
			              <a href="%s"
			                 style="display:inline-block;padding:14px 40px;background-color:#FFAE42;color:#ffffff;
			                        font-size:16px;font-weight:700;text-decoration:none;border-radius:12px;
			                        letter-spacing:-0.3px;">
			                이메일 확인
			              </a>
			            </td>
			          </tr>
			          <tr>
			            <td style="padding-bottom:20px;">
			              <hr style="border:none;border-top:1px solid #e5e7eb;margin:0;">
			            </td>
			          </tr>
			          <tr>
			            <td style="padding-bottom:8px;">
			              <p style="margin:0;font-size:12px;color:#9ca3af;">
			                링크 만료 시각: %s
			              </p>
			            </td>
			          </tr>
			          <tr>
			            <td>
			              <p style="margin:0;font-size:12px;color:#9ca3af;">
			                버튼이 동작하지 않으면
			                <a href="%s" style="color:#FFAE42;text-decoration:underline;">여기를 클릭</a>하세요.
			              </p>
			            </td>
			          </tr>
			        </table>
			      </td>
			    </tr>
			  </table>
			</body>
			</html>
			"""
			.formatted(escapedVerificationUrl, formattedExpiresAt, escapedVerificationUrl);

		send(email, subject, textBody, htmlBody);
	}

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

	private String escapeHtmlAttribute(String value) {
		if (value == null) {
			return "";
		}
		return value
			.replace("&", "&amp;")
			.replace("\"", "&quot;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}
