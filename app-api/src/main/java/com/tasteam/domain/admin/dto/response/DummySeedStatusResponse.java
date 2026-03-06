package com.tasteam.domain.admin.dto.response;

public record DummySeedStatusResponse(
	String status,
	String currentStep,
	int completedSteps,
	int totalSteps,
	String errorMessage,
	AdminDummySeedResponse result,
	Long elapsedMs) {
}
