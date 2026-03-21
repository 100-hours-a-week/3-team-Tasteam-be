package com.tasteam.domain.analytics.export;

public enum RawDataType {
	RESTAURANTS("restaurants"),
	MENUS("menus");

	private final String pathSegment;

	RawDataType(String pathSegment) {
		this.pathSegment = pathSegment;
	}

	public String pathSegment() {
		return pathSegment;
	}
}
