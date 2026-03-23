package com.tasteam.infra.webhook.discord;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.tasteam.infra.webhook.WebhookProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.webhook", name = "enabled", havingValue = "true")
public class BatchReportDiscordWebhookClient {

	private final RestTemplate webhookRestTemplate;
	private final WebhookProperties properties;

	public void send(DiscordMessage message) {
		String url = properties.getDiscord().getBatchReportUrl();
		if (!StringUtils.hasText(url)) {
			log.debug("batch-report-url이 설정되지 않아 배치 리포트 웹훅을 건너뜁니다.");
			return;
		}

		int maxAttempts = properties.getRetry().getMaxAttempts();
		long backoffMs = properties.getRetry().getBackoffMs();

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				sendJsonOnly(url, message);
				log.info("배치 리포트 웹훅 전송 성공. attempt={}/{}", attempt, maxAttempts);
				return;
			} catch (Exception e) {
				log.error("배치 리포트 웹훅 전송 실패. attempt={}/{}, error={}",
					attempt, maxAttempts, e.getMessage());

				if (attempt < maxAttempts) {
					try {
						Thread.sleep(backoffMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						log.error("배치 리포트 웹훅 재시도가 중단되었습니다.", ie);
						return;
					}
				} else {
					log.error("{}회 시도 후 배치 리포트 웹훅 전송 실패", maxAttempts, e);
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
}
