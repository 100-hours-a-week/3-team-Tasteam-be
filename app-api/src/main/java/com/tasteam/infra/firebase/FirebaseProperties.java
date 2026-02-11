package com.tasteam.infra.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.firebase")
public class FirebaseProperties {

	private boolean enabled;
	private String projectId;
	private String serviceAccountBase64;
	private String serviceAccountPath;
}
