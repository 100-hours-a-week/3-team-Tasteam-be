package com.tasteam.domain.recommendation.importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public interface RecommendationResultCsvReader {

	void read(InputStream inputStream, Consumer<ParsedRecommendationCsvRow> consumer) throws IOException;
}
