package com.tasteam.infra.webhook.discord;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.infra.webhook.WebhookClient;
import com.tasteam.infra.webhook.WebhookMessage;
import com.tasteam.infra.webhook.WebhookProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.webhook", name = "enabled", havingValue = "true")
public class DiscordWebhookClient implements WebhookClient {

	private final RestTemplate webhookRestTemplate;
	private final WebhookProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public void send(WebhookMessage message) {
		if (!isEnabled()) {
			log.warn("Discord 웹훅 URL이 설정되지 않았습니다. 웹훅 전송을 건너뜁니다.");
			return;
		}

		String url = properties.getDiscord().getUrl();
		int maxAttempts = properties.getRetry().getMaxAttempts();
		long backoffMs = properties.getRetry().getBackoffMs();

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				DiscordMessage discordMessage = DiscordMessage.from(message);

				if (StringUtils.hasText(message.stackTrace())) {
					sendWithAttachment(url, discordMessage, message);
				} else {
					sendJsonOnly(url, discordMessage);
				}

				log.info("웹훅 전송 성공. provider=discord, title={}, attempt={}/{}",
					message.title(), attempt, maxAttempts);
				return;

			} catch (Exception e) {
				log.error("웹훅 전송 실패. provider=discord, title={}, attempt={}/{}, error={}",
					message.title(), attempt, maxAttempts, e.getMessage());

				if (attempt < maxAttempts) {
					try {
						Thread.sleep(backoffMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						log.error("웹훅 재시도가 중단되었습니다.", ie);
						return;
					}
				} else {
					log.error("{}회 시도 후 웹훅 전송 실패. provider=discord, title={}",
						maxAttempts, message.title(), e);
				}
			}
		}
	}

	private void sendJsonOnly(String url, DiscordMessage discordMessage) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<DiscordMessage> request = new HttpEntity<>(discordMessage, headers);
		webhookRestTemplate.postForEntity(url, request, String.class);
	}

	private void sendWithAttachment(String url, DiscordMessage discordMessage, WebhookMessage message) {
		try {
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

			String payloadJson = objectMapper.writeValueAsString(discordMessage);
			body.add("payload_json", payloadJson);

			String filename = "stacktrace_" + message.timestamp().toEpochMilli() + ".txt";
			byte[] fileContent = message.stackTrace().getBytes(StandardCharsets.UTF_8);
			ByteArrayResource fileResource = new ByteArrayResource(fileContent) {
				@Override
				public String getFilename() {
					return filename;
				}
			};
			body.add("files[0]", fileResource);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);

			HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
			webhookRestTemplate.postForEntity(url, request, String.class);

		} catch (Exception e) {
			log.error("파일 첨부 실패, JSON만 전송 시도", e);
			sendJsonOnly(url, discordMessage);
		}
	}

	@Override
	public boolean isEnabled() {
		return properties.isEnabled()
			&& "discord".equalsIgnoreCase(properties.getProvider())
			&& StringUtils.hasText(properties.getDiscord().getUrl());
	}
}
