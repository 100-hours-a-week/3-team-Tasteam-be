package com.tasteam.domain.search.repository;

public enum SearchQueryStrategy {
	ONE_STEP,
	TWO_STEP,
	JOIN_AGGREGATE,
	HYBRID_SPLIT_CANDIDATES,
	GEO_FIRST_HYBRID,
	READ_MODEL_TWO_STEP,
	MV_SINGLE_PASS
}
