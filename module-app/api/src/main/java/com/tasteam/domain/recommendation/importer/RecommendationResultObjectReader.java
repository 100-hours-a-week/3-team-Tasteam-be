package com.tasteam.domain.recommendation.importer;

import java.io.InputStream;

public interface RecommendationResultObjectReader {

	InputStream openStream(String s3Uri);
}
