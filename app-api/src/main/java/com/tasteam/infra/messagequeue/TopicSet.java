package com.tasteam.infra.messagequeue;

public record TopicSet(String main, String dlq) {

	public TopicSet {
		if (main == null || main.isBlank()) {
			throw new IllegalArgumentException("main topic은 필수입니다");
		}
		if (dlq == null || dlq.isBlank()) {
			throw new IllegalArgumentException("dlq topic은 필수입니다");
		}
	}

	public String retry(int attempt) {
		if (attempt < 1) {
			throw new IllegalArgumentException("retry attempt는 1 이상이어야 합니다");
		}
		return main + ".retry." + attempt;
	}
}
