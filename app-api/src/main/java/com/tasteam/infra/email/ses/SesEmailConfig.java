package com.tasteam.infra.email.ses;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.tasteam.infra.email.EmailProperties;

@Configuration
@Profile("!test")
@ConditionalOnProperty(prefix = "tasteam.email", name = "type", havingValue = "ses")
public class SesEmailConfig {

	@Bean("sesAwsCredentialsProvider")
	public AWSCredentialsProvider sesAwsCredentialsProvider(EmailProperties properties) {
		if (properties.hasStaticCredentials()) {
			return new AWSStaticCredentialsProvider(
				new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey()));
		}
		return DefaultAWSCredentialsProviderChain.getInstance();
	}

	@Bean
	public AmazonSimpleEmailService amazonSes(
		EmailProperties properties,
		@Qualifier("sesAwsCredentialsProvider")
		AWSCredentialsProvider credentialsProvider) {
		Assert.hasText(properties.getRegion(), "tasteam.email.region은 필수입니다");
		return AmazonSimpleEmailServiceClientBuilder.standard()
			.withRegion(properties.getRegion())
			.withCredentials(credentialsProvider)
			.build();
	}
}
