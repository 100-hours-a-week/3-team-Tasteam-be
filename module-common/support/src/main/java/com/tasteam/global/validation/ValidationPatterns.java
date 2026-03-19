package com.tasteam.global.validation;

public final class ValidationPatterns {

	public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
	public static final String URL_PATTERN = "^https?://.*";
	public static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

	public static final int NICKNAME_MAX_LENGTH = 30;
	public static final int INTRODUCTION_MAX_LENGTH = 500;
	public static final int PASSWORD_MIN_LENGTH = 8;
}
