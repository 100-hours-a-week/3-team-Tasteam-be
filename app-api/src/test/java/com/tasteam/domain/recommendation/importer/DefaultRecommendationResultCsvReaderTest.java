package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;

@UnitTest
@DisplayName("[유닛](Recommendation) DefaultRecommendationResultCsvReader 단위 테스트")
class DefaultRecommendationResultCsvReaderTest {

	private final DefaultRecommendationResultCsvReader reader = new DefaultRecommendationResultCsvReader();

	@Test
	@DisplayName("CSV를 스트리밍으로 읽고 generated_at/expires_at을 파싱한다")
	void read_parsesRows() throws Exception {
		String csv = """
			user_id,anonymous_id,restaurant_id,score,rank,context_snapshot,pipeline_version,generated_at,expires_at
			1,,101,0.892,1,{},deepfm-1,2026-02-27T14:00:00.000000+00:00,2026-02-28T14:00:00.000000+00:00
			,anon_003,303,0.543,1,{},deepfm-1,2026-02-27T14:00:00.000000+00:00,2026-02-28T14:00:00.000000+00:00
			""";

		List<ParsedRecommendationCsvRow> rows = new ArrayList<>();
		reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), rows::add);

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).userId()).isEqualTo("1");
		assertThat(rows.get(1).anonymousId()).isEqualTo("anon_003");
		assertThat(rows.get(0).contextSnapshot()).isEqualTo("{}");
		assertThat(rows.get(1).userId()).isEmpty();
		assertThat(rows.get(0).generatedAt()).isEqualTo(java.time.Instant.parse("2026-02-27T14:00:00Z"));
		assertThat(rows.get(0).expiresAt()).isEqualTo(java.time.Instant.parse("2026-02-28T14:00:00Z"));
	}

	@Test
	@DisplayName("필수 헤더가 없으면 CSV_FORMAT_INVALID 예외를 던진다")
	void read_throwsWhenMissingRequiredHeader() {
		String csv = "user_id,restaurant_id,score,rank,generated_at,expires_at\n1,101,0.9,1,2026-02-27T14:00:00+00:00,2026-02-28T14:00:00+00:00\n";

		assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), row -> {}))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("필수 헤더");
	}
}
