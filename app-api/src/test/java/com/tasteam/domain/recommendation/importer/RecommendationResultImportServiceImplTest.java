package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModel;
import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModelStatus;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationJdbcRepository;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationRow;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationModelRepository;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationResultImportServiceImpl 단위 테스트")
class RecommendationResultImportServiceImplTest {

	@Test
	@DisplayName("요청 모델이 없으면 MODEL_NOT_FOUND 예외를 던진다")
	void ingest_throwsWhenModelNotFound() {
		RestaurantRecommendationModelRepository repository = mock(RestaurantRecommendationModelRepository.class);
		RestaurantRecommendationJdbcRepository jdbcRepository = mock(RestaurantRecommendationJdbcRepository.class);
		RecommendationResultObjectReader objectReader = mock(RecommendationResultObjectReader.class);
		RecommendationResultCsvReader csvReader = mock(RecommendationResultCsvReader.class);
		RecommendationImportRowValidator validator = mock(RecommendationImportRowValidator.class);
		TransactionOperations txOps = mock(TransactionOperations.class);
		when(repository.findById("deepfm-1")).thenReturn(Optional.empty());
		RecommendationResultImportServiceImpl service = new RecommendationResultImportServiceImpl(
			repository,
			jdbcRepository,
			objectReader,
			csvReader,
			validator,
			txOps);

		assertThatThrownBy(
			() -> service
				.importResults(new RecommendationResultImportRequest("deepfm-1", "s3://bucket/result.csv", null)))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("version=deepfm-1");
	}

	@Test
	@DisplayName("모델이 존재하면 LOADING 후 READY 상태로 전이하고 결과를 반환한다")
	void ingest_marksLoadingThenReady() throws Exception {
		RestaurantRecommendationModelRepository repository = mock(RestaurantRecommendationModelRepository.class);
		RestaurantRecommendationJdbcRepository jdbcRepository = mock(RestaurantRecommendationJdbcRepository.class);
		RecommendationResultObjectReader objectReader = mock(RecommendationResultObjectReader.class);
		RecommendationResultCsvReader csvReader = mock(RecommendationResultCsvReader.class);
		RecommendationImportRowValidator validator = mock(RecommendationImportRowValidator.class);
		TransactionOperations txOps = passthroughTxOps();
		RestaurantRecommendationModel model = RestaurantRecommendationModel.loading("deepfm-1");
		RestaurantRecommendationRow row = new RestaurantRecommendationRow(
			1L,
			null,
			11L,
			0.91,
			1,
			"{}",
			"deepfm-1",
			java.time.Instant.parse("2026-03-03T10:00:00Z"),
			java.time.Instant.parse("2026-03-04T10:00:00Z"));

		when(repository.findById("deepfm-1")).thenReturn(Optional.of(model));
		when(repository.save(model)).thenReturn(model);
		when(objectReader.openStream("s3://bucket/result.csv")).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(validator.validateAndConvertOrNull(any(ParsedRecommendationCsvRow.class), eq("deepfm-1"))).thenReturn(row);
		when(jdbcRepository.batchInsert(any())).thenReturn(1);
		RecommendationResultImportServiceImpl service = new RecommendationResultImportServiceImpl(
			repository,
			jdbcRepository,
			objectReader,
			csvReader,
			validator,
			txOps);

		doAnswer(invocation -> {
			@SuppressWarnings("unchecked") Consumer<ParsedRecommendationCsvRow> consumer = invocation.getArgument(1);
			consumer.accept(new ParsedRecommendationCsvRow(
				2L,
				"1",
				"",
				"11",
				"0.91",
				"1",
				"{}",
				"deepfm-1",
				java.time.Instant.parse("2026-03-03T10:00:00Z"),
				java.time.Instant.parse("2026-03-04T10:00:00Z")));
			return null;
		}).when(csvReader).read(any(), any());

		RecommendationResultImportResult result = service.importResults(
			new RecommendationResultImportRequest("deepfm-1", "s3://bucket/result.csv", "req-1"));

		assertThat(result.modelVersion()).isEqualTo("deepfm-1");
		assertThat(result.totalRows()).isEqualTo(1);
		assertThat(result.insertedRows()).isEqualTo(1);
		assertThat(result.skippedRows()).isZero();
		assertThat(model.getStatus()).isEqualTo(RestaurantRecommendationModelStatus.READY);
		verify(repository).findById("deepfm-1");
		verify(repository, times(2)).save(model);
		verify(jdbcRepository).deleteByPipelineVersion("deepfm-1");
	}

	private TransactionOperations passthroughTxOps() {
		TransactionOperations txOps = mock(TransactionOperations.class);
		when(txOps.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(null);
		});
		return txOps;
	}
}
