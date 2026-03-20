package com.tasteam.infra.firebase;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

	@Bean
	@ConditionalOnProperty(prefix = "tasteam.firebase", name = "enabled", havingValue = "true")
	public FirebaseApp firebaseApp(FirebaseProperties properties) throws Exception {
		if (!FirebaseApp.getApps().isEmpty()) {
			return FirebaseApp.getInstance();
		}

		try (InputStream credentialsStream = resolveCredentials(properties)) {
			GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
			FirebaseOptions.Builder builder = FirebaseOptions.builder()
				.setCredentials(credentials);

			if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) {
				builder.setProjectId(properties.getProjectId());
			}

			return FirebaseApp.initializeApp(builder.build());
		}
	}

	@Bean
	@ConditionalOnProperty(prefix = "tasteam.firebase", name = "enabled", havingValue = "true")
	public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
		return FirebaseMessaging.getInstance(firebaseApp);
	}

	private InputStream resolveCredentials(FirebaseProperties properties) throws Exception {
		String base64 = properties.getServiceAccountBase64();
		if (base64 != null && !base64.isBlank()) {
			byte[] decoded = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
			return new ByteArrayInputStream(decoded);
		}

		String path = properties.getServiceAccountPath();
		if (path != null && !path.isBlank()) {
			return new FileInputStream(path);
		}

		throw new IllegalStateException("Firebase service account credentials not configured.");
	}
}
