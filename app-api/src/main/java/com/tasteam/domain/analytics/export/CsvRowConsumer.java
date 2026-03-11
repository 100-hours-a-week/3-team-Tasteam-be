package com.tasteam.domain.analytics.export;

import java.util.List;

@FunctionalInterface
public interface CsvRowConsumer {

	void accept(List<String> row);
}
