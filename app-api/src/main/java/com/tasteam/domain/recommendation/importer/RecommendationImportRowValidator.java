package com.tasteam.domain.recommendation.importer;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationRow;

@Component
public class RecommendationImportRowValidator {

	public RestaurantRecommendationRow validateAndConvertOrNull(
		ParsedRecommendationCsvRow row,
		String expectedModelVersion) {
		Objects.requireNonNull(row, "row는 null일 수 없습니다.");

		if (!StringUtils.hasText(row.userId())) {
			return null;
		}

		validatePipelineVersion(row, expectedModelVersion);
		long userId = parseLong(row.userId(), "user_id", row.lineNumber());
		long restaurantId = parseLong(row.restaurantId(), "restaurant_id", row.lineNumber());
		double score = parseDouble(row.score(), "score", row.lineNumber());
		int rank = parseInt(row.rank(), "rank", row.lineNumber());
		if (rank <= 0) {
			throw RecommendationBusinessException.resultValidationFailed(
				"rank는 1 이상이어야 합니다. line=" + row.lineNumber() + ", value=" + rank);
		}
		if (!row.expiresAt().isAfter(row.generatedAt())) {
			throw RecommendationBusinessException.resultValidationFailed(
				"expires_at은 generated_at 이후여야 합니다. line=" + row.lineNumber());
		}
		return new RestaurantRecommendationRow(userId, restaurantId, score, rank, row.generatedAt(), row.expiresAt());
	}

	private void validatePipelineVersion(ParsedRecommendationCsvRow row, String expectedModelVersion) {
		if (!StringUtils.hasText(row.pipelineVersion())) {
			throw RecommendationBusinessException.resultValidationFailed(
				"pipeline_version은 비어 있을 수 없습니다. line=" + row.lineNumber());
		}
		if (!row.pipelineVersion().equals(expectedModelVersion)) {
			throw RecommendationBusinessException.pipelineVersionMismatch(expectedModelVersion, row.pipelineVersion());
		}
	}

	private long parseLong(String value, String columnName, long lineNumber) {
		if (!StringUtils.hasText(value)) {
			throw RecommendationBusinessException.resultValidationFailed(
				columnName + " 값이 비어 있습니다. line=" + lineNumber);
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException ex) {
			throw RecommendationBusinessException.resultValidationFailed(
				columnName + " 숫자 파싱에 실패했습니다. line=" + lineNumber + ", value=" + value);
		}
	}

	private int parseInt(String value, String columnName, long lineNumber) {
		if (!StringUtils.hasText(value)) {
			throw RecommendationBusinessException.resultValidationFailed(
				columnName + " 값이 비어 있습니다. line=" + lineNumber);
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ex) {
			throw RecommendationBusinessException.resultValidationFailed(
				columnName + " 숫자 파싱에 실패했습니다. line=" + lineNumber + ", value=" + value);
		}
	}

	private double parseDouble(String value, String columnName, long lineNumber) {
		if (!StringUtils.hasText(value)) {
			throw RecommendationBusinessException.resultValidationFailed(
				columnName + " 값이 비어 있습니다. line=" + lineNumber);
		}
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException ex) {
			throw RecommendationBusinessException.resultValidationFailed(
				columnName + " 숫자 파싱에 실패했습니다. line=" + lineNumber + ", value=" + value);
		}
	}
}
