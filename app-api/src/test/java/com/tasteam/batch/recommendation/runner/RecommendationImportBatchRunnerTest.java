package com.tasteam.batch.recommendation.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.importer.RecommendationResultImportFacade;
import com.tasteam.domain.recommendation.importer.RecommendationResultImportFacadeCommand;
import com.tasteam.domain.recommendation.importer.RecommendationResultImportResult;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationImportBatchRunner 단위 테스트")
class RecommendationImportBatchRunnerTest {

	@Test
	@DisplayName("온디맨드 실행 시 facade에 요청을 위임한다")
	void runOnDemand_delegatesToFacade() {
		RecommendationResultImportFacade facade = mock(RecommendationResultImportFacade.class);
		RecommendationImportBatchRunner runner = new RecommendationImportBatchRunner(facade);
		RecommendationResultImportResult expected = new RecommendationResultImportResult("deepfm-1", 3, 3, 0);
		when(facade.importResults(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

		RecommendationResultImportResult result = runner.runOnDemand("deepfm-1", "s3://bucket/prefix/", "req-1");

		ArgumentCaptor<RecommendationResultImportFacadeCommand> captor = ArgumentCaptor.forClass(
			RecommendationResultImportFacadeCommand.class);
		verify(facade).importResults(captor.capture());
		RecommendationResultImportFacadeCommand command = captor.getValue();
		assertThat(command.requestedModelVersion()).isEqualTo("deepfm-1");
		assertThat(command.resultS3Uri()).isEqualTo("s3://bucket/prefix/");
		assertThat(command.requestId()).isEqualTo("req-1");
		assertThat(result).isEqualTo(expected);
	}

	@Test
	@DisplayName("실행 중 상태를 강제로 만들면 중복 실행 예외를 반환한다")
	void runOnDemand_throwsBusinessExceptionWhenDuplicate() throws Exception {
		RecommendationResultImportFacade facade = mock(RecommendationResultImportFacade.class);
		RecommendationImportBatchRunner runner = new RecommendationImportBatchRunner(facade);

		var field = RecommendationImportBatchRunner.class.getDeclaredField("runningModelVersions");
		field.setAccessible(true);
		@SuppressWarnings("unchecked") java.util.Set<String> set = (java.util.Set<String>)field.get(runner);
		set.add("deepfm-1");

		assertThatThrownBy(() -> runner.runOnDemand("deepfm-1", "s3://bucket/prefix/", "req-1"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("이미 실행 중");
	}
}
