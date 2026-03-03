package com.tasteam.domain.recommendation.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;

@Component
public class DefaultRecommendationResultCsvReader implements RecommendationResultCsvReader {

	private static final String USER_ID = "user_id";
	private static final String RESTAURANT_ID = "restaurant_id";
	private static final String SCORE = "score";
	private static final String RANK = "rank";
	private static final String PIPELINE_VERSION = "pipeline_version";
	private static final String GENERATED_AT = "generated_at";
	private static final String EXPIRES_AT = "expires_at";

	@Override
	public void read(InputStream inputStream, Consumer<ParsedRecommendationCsvRow> consumer) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String headerLine = reader.readLine();
			if (headerLine == null) {
				throw RecommendationBusinessException.csvFormatInvalid("CSV 헤더가 없습니다.");
			}
			Map<String, Integer> headerIndexMap = parseHeaderIndexMap(headerLine);
			validateRequiredHeaders(headerIndexMap);

			String line;
			long lineNumber = 1L;
			while ((line = reader.readLine()) != null) {
				lineNumber += 1;
				if (line.isBlank()) {
					continue;
				}
				List<String> fields = parseCsvLine(line);
				consumer.accept(new ParsedRecommendationCsvRow(
					lineNumber,
					valueAt(fields, headerIndexMap, USER_ID, lineNumber),
					valueAt(fields, headerIndexMap, RESTAURANT_ID, lineNumber),
					valueAt(fields, headerIndexMap, SCORE, lineNumber),
					valueAt(fields, headerIndexMap, RANK, lineNumber),
					valueAt(fields, headerIndexMap, PIPELINE_VERSION, lineNumber),
					parseInstant(valueAt(fields, headerIndexMap, GENERATED_AT, lineNumber), GENERATED_AT, lineNumber),
					parseInstant(valueAt(fields, headerIndexMap, EXPIRES_AT, lineNumber), EXPIRES_AT, lineNumber)));
			}
		}
	}

	private Map<String, Integer> parseHeaderIndexMap(String headerLine) {
		List<String> headers = parseCsvLine(headerLine);
		Map<String, Integer> indexMap = new HashMap<>();
		for (int i = 0; i < headers.size(); i++) {
			indexMap.put(headers.get(i).trim(), i);
		}
		return indexMap;
	}

	private void validateRequiredHeaders(Map<String, Integer> headerIndexMap) {
		for (String requiredHeader : List.of(USER_ID, RESTAURANT_ID, SCORE, RANK, PIPELINE_VERSION, GENERATED_AT,
			EXPIRES_AT)) {
			if (!headerIndexMap.containsKey(requiredHeader)) {
				throw RecommendationBusinessException.csvFormatInvalid("필수 헤더가 없습니다: " + requiredHeader);
			}
		}
	}

	private String valueAt(List<String> fields, Map<String, Integer> headerIndexMap, String headerName,
		long lineNumber) {
		Integer index = headerIndexMap.get(headerName);
		if (index == null || index < 0 || index >= fields.size()) {
			throw RecommendationBusinessException.csvFormatInvalid(
				"CSV 컬럼 파싱에 실패했습니다. line=" + lineNumber + ", column=" + headerName);
		}
		return fields.get(index);
	}

	private Instant parseInstant(String value, String columnName, long lineNumber) {
		try {
			return OffsetDateTime.parse(value).toInstant();
		} catch (DateTimeParseException ex) {
			throw RecommendationBusinessException.csvFormatInvalid(
				"시간 형식이 올바르지 않습니다. line=" + lineNumber + ", column=" + columnName + ", value=" + value);
		}
	}

	private List<String> parseCsvLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i += 1;
				} else {
					inQuotes = !inQuotes;
				}
				continue;
			}
			if (c == ',' && !inQuotes) {
				fields.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(c);
		}
		if (inQuotes) {
			throw RecommendationBusinessException.csvFormatInvalid("따옴표가 닫히지 않은 CSV 라인이 있습니다.");
		}
		fields.add(current.toString());
		return fields;
	}
}
