package com.tasteam.infra.email.ses;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.tasteam.infra.email.EmailNotificationProperties;

@Configuration
@Profile("!test")
@ConditionalOnProperty(prefix = "tasteam.notification.email", name = "provider", havingValue = "ses")
public class SesEmailConfig {

	@Bean("sesAwsCredentialsProvider")
	public AWSCredentialsProvider sesAwsCredentialsProvider(EmailNotificationProperties properties) {
		String accessKey = properties.getSes().getAccessKey();
		String secretKey = properties.getSes().getSecretKey();
		if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
			return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
		}
		return DefaultAWSCredentialsProviderChain.getInstance();
	}

	@Bean
	public AmazonSimpleEmailService amazonSes(
		EmailNotificationProperties properties,
		@Qualifier("sesAwsCredentialsProvider")
		AWSCredentialsProvider credentialsProvider) {
		Assert.hasText(properties.getSes().getRegion(),
			"SES 사용 시 NOTIFICATION_EMAIL_SES_REGION 설정 필수");
		Assert.hasText(properties.getSes().getFromAddress(),
			"SES 사용 시 NOTIFICATION_EMAIL_SES_FROM_ADDRESS 설정 필수");
		return AmazonSimpleEmailServiceClientBuilder.standard()
			.withRegion(properties.getSes().getRegion())
			.withCredentials(credentialsProvider)
			.build();
	}
}
