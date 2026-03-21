package com.tasteam.config.fake;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.test.context.DynamicPropertyRegistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Simple fake OAuth2 provider that stands in for Google/GitHub during integration tests.
 * <p>
 * Start this provider before the test suite and override the Spring security properties with
 * {@code DynamicPropertyRegistry} so that {@code OAuthLoginService} talks to the embedded endpoints.
 *
 * <pre>{@code
 * private static final FakeOAuth2Provider fakeGoogle = FakeOAuth2Provider.google()
 *         .start();
 *
 * @DynamicPropertySource
 * static void oauthProperties(DynamicPropertyRegistry registry) {
 *     fakeGoogle.register(registry);
 * }
 * }</pre>
 */
public final class FakeOAuth2Provider implements AutoCloseable {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final MockWebServer server = new MockWebServer();
	private final Dispatcher dispatcher;

	private final String registrationId;
	private final Map<String, Object> userInfo;
	private final String accessToken;
	private final String refreshToken;
	private final int expiresInSeconds;
	private final String tokenType;
	private final String tokenPath;
	private final String userInfoPath;
	private final String authorizationPath;
	private final String userNameAttribute;

	private boolean started = false;

	private FakeOAuth2Provider(Builder builder) {
		this.registrationId = builder.registrationId;
		this.userInfo = new LinkedHashMap<>(builder.userInfo);
		this.accessToken = builder.accessToken;
		this.refreshToken = builder.refreshToken;
		this.expiresInSeconds = builder.expiresInSeconds;
		this.tokenType = builder.tokenType;
		this.tokenPath = builder.tokenPath;
		this.userInfoPath = builder.userInfoPath;
		this.authorizationPath = builder.authorizationPath;
		this.userNameAttribute = builder.userNameAttribute;
		this.dispatcher = createDispatcher();
	}

	public static Builder builder(String registrationId) {
		return new Builder(registrationId);
	}

	public static FakeOAuth2Provider google() {
		return builder("google").build();
	}

	public FakeOAuth2Provider start() {
		if (started) {
			return this;
		}
		server.setDispatcher(dispatcher);
		try {
			server.start();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot start fake OAuth2 provider", e);
		}
		started = true;
		return this;
	}

	@Override
	public void close() throws IOException {
		started = false;
		server.shutdown();
	}

	public String getTokenUri() {
		return resolve(tokenPath);
	}

	public String getUserInfoUri() {
		return resolve(userInfoPath);
	}

	public String getAuthorizationUri() {
		return resolve(authorizationPath);
	}

	public String getUserNameAttribute() {
		return userNameAttribute;
	}

	public void register(DynamicPropertyRegistry registry) {
		registry.add("spring.security.oauth2.client.provider." + registrationId + ".authorization-uri",
			this::getAuthorizationUri);
		registry.add("spring.security.oauth2.client.provider." + registrationId + ".token-uri", this::getTokenUri);
		registry.add("spring.security.oauth2.client.provider." + registrationId + ".user-info-uri",
			this::getUserInfoUri);
		registry.add("spring.security.oauth2.client.provider." + registrationId + ".user-name-attribute",
			this::getUserNameAttribute);
	}

	private String resolve(String path) {
		ensureStarted();
		HttpUrl url = server.url(path);
		return url.toString();
	}

	private void ensureStarted() {
		if (!started) {
			throw new IllegalStateException(
				"FakeOAuth2Provider has not been started. Call start() before retrieving URIs.");
		}
	}

	private Dispatcher createDispatcher() {
		return new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				if (request == null || request.getRequestUrl() == null) {
					return new MockResponse().setResponseCode(404);
				}
				String path = request.getRequestUrl().encodedPath();

				if (path.equals(tokenPath)) {
					return createTokenResponse();
				}
				if (path.equals(userInfoPath)) {
					return createUserInfoResponse();
				}
				if (path.equals(authorizationPath)) {
					return createAuthorizationResponse(request);
				}
				return new MockResponse().setResponseCode(404);
			}
		};
	}

	private MockResponse createTokenResponse() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("access_token", accessToken);
		payload.put("token_type", tokenType);
		payload.put("expires_in", expiresInSeconds);
		payload.put("refresh_token", refreshToken);
		payload.put("scope", "openid email profile");
		return jsonResponse(payload);
	}

	private MockResponse createUserInfoResponse() {
		return jsonResponse(userInfo);
	}

	private MockResponse createAuthorizationResponse(RecordedRequest request) {
		HttpUrl requestUrl = request.getRequestUrl();
		if (requestUrl == null) {
			return failureResponse("authorization request missing url");
		}
		String redirect = requestUrl.queryParameter("redirect_uri");
		String state = requestUrl.queryParameter("state");

		if (redirect == null) {
			return failureResponse("redirect_uri is required");
		}

		HttpUrl target = HttpUrl.get(redirect);
		HttpUrl.Builder builder = target.newBuilder()
			.addQueryParameter("code", "fake-code");
		if (state != null) {
			builder.addQueryParameter("state", state);
		}

		return new MockResponse()
			.setResponseCode(302)
			.addHeader("Location", builder.build().toString());
	}

	private MockResponse failureResponse(String message) {
		return new MockResponse()
			.setResponseCode(400)
			.setHeader("Content-Type", "text/plain")
			.setBody(message);
	}

	private MockResponse jsonResponse(Object payload) {
		try {
			return new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json;charset=UTF-8")
				.setBody(MAPPER.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Unable to serialize fake OAuth2 response", e);
		}
	}

	public static final class Builder {

		private final String registrationId;
		private final Map<String, Object> userInfo = new LinkedHashMap<>();
		private String accessToken = "fake-access-token";
		private String refreshToken = "fake-refresh-token";
		private int expiresInSeconds = 3600;
		private String tokenType = "Bearer";
		private String tokenPath = "/oauth2/token";
		private String userInfoPath = "/oauth2/v3/userinfo";
		private String authorizationPath = "/oauth2/authorize";
		private String userNameAttribute = "sub";

		private Builder(String registrationId) {
			this.registrationId = registrationId;
			userInfo.put("sub", "fake-" + registrationId + "-sub");
			userInfo.put("email", registrationId + "-user@example.com");
			userInfo.put("name", "Fake " + registrationId + " User");
		}

		public Builder accessToken(String accessToken) {
			this.accessToken = accessToken;
			return this;
		}

		public Builder refreshToken(String refreshToken) {
			this.refreshToken = refreshToken;
			return this;
		}

		public Builder expiresInSeconds(int expiresInSeconds) {
			this.expiresInSeconds = expiresInSeconds;
			return this;
		}

		public Builder tokenPath(String tokenPath) {
			this.tokenPath = tokenPath;
			return this;
		}

		public Builder userInfoPath(String userInfoPath) {
			this.userInfoPath = userInfoPath;
			return this;
		}

		public Builder authorizationPath(String authorizationPath) {
			this.authorizationPath = authorizationPath;
			return this;
		}

		public Builder userNameAttribute(String userNameAttribute) {
			this.userNameAttribute = userNameAttribute;
			return this;
		}

		public Builder userInfoAttributes(Map<String, Object> attributes) {
			userInfo.clear();
			userInfo.putAll(attributes);
			return this;
		}

		public Builder userInfoEntry(String key, Object value) {
			userInfo.put(key, value);
			return this;
		}

		public FakeOAuth2Provider build() {
			return new FakeOAuth2Provider(this);
		}
	}
}
