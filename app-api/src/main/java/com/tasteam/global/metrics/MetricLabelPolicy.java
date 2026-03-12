package com.tasteam.global.metrics;

import java.util.Set;

/**
 * 메트릭 라벨 카디널리티 제어를 위한 허용 라벨 정책.
 */
public final class MetricLabelPolicy {

	private static final Set<String> ALLOWED_LABELS = Set.of(
		"environment",
		"instance",
		"cache",
		"result",
		"state",
		"topic",
		"provider",
		"target",
		"reason",
		"executor",
		"method",
		"uri",
		"domain",
		"outcome",
		"read_only",
		"method_name",
		"partitionId",
		"streamKey",
		"errorType");

	private static final Set<String> FORBIDDEN_LABELS = Set.of(
		"eventId",
		"memberId",
		"chatRoomId");

	private MetricLabelPolicy() {}

	public static void validate(String metricName, String... tags) {
		if (tags == null || tags.length == 0) {
			return;
		}
		if (tags.length % 2 != 0) {
			throw new IllegalArgumentException("메트릭 라벨은 key/value 쌍으로 전달해야 합니다. metric=" + metricName);
		}
		for (int i = 0; i < tags.length; i += 2) {
			String labelKey = tags[i];
			if (labelKey == null || labelKey.isBlank()) {
				throw new IllegalArgumentException("메트릭 라벨 key가 비어 있습니다. metric=" + metricName);
			}
			if (FORBIDDEN_LABELS.contains(labelKey)) {
				throw new IllegalArgumentException("금지된 메트릭 라벨이 사용되었습니다. metric=%s, label=%s"
					.formatted(metricName, labelKey));
			}
			if (!ALLOWED_LABELS.contains(labelKey)) {
				throw new IllegalArgumentException("허용되지 않은 메트릭 라벨입니다. metric=%s, label=%s"
					.formatted(metricName, labelKey));
			}
		}
	}
}
