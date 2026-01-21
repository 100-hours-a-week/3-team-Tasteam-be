package com.tasteam.infra.storage.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.tasteam.infra.storage.StorageProperties;

@Configuration
@Profile("!test")
public class S3StorageConfig {

	@Bean
	public AmazonS3 amazonS3(StorageProperties properties) {
		Assert.hasText(properties.getRegion(), "tasteam.storage.region은 필수입니다");

		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
			.withRegion(properties.getRegion());

		if (properties.hasStaticCredentials()) {
			builder.withCredentials(new AWSStaticCredentialsProvider(
				new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey())));
		}

		return builder.build();
	}
}
