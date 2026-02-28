package com.tasteam.infra.storage.s3;

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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.tasteam.infra.storage.StorageProperties;

@Configuration
@Profile("!test")
@ConditionalOnProperty(prefix = "tasteam.storage", name = "type", havingValue = "s3")
public class S3StorageConfig {

	@Bean
	public AWSCredentialsProvider s3AwsCredentialsProvider(StorageProperties properties) {
		if (properties.hasStaticCredentials()) {
			return new AWSStaticCredentialsProvider(
				new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey()));
		}
		return DefaultAWSCredentialsProviderChain.getInstance();
	}

	@Bean
	public AmazonS3 amazonS3(
		StorageProperties properties,
		@Qualifier("s3AwsCredentialsProvider")
		AWSCredentialsProvider credentialsProvider) {
		Assert.hasText(properties.getRegion(), "tasteam.storage.region은 필수입니다");

		return AmazonS3ClientBuilder.standard()
			.withRegion(properties.getRegion())
			.withCredentials(credentialsProvider)
			.build();
	}
}
