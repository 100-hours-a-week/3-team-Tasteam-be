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
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;
import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModel;
import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModelStatus;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationJdbcRepository;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationRow;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationModelRepository;
import com.tasteam.global.exception.code.RecommendationErrorCode;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationResultImportServiceImpl 단위 테스트")
class RecommendationResultImportServiceImplTest {

	@Test
	@DisplayName("요청 모델이 없으면 LOADING 상태 모델을 생성하고 import를 진행한다")
	void importResults_createsLoadingModelWhenMissing() throws Exception {
		BatchExecutionRepository batchExecutionRepository = mock(BatchExecutionRepository.class);
		RestaurantRecommendationModelRepository repository = mock(RestaurantRecommendationModelRepository.class);
		RestaurantRecommendationJdbcRepository jdbcRepository = mock(RestaurantRecommendationJdbcRepository.class);
		RecommendationResultObjectReader objectReader = mock(RecommendationResultObjectReader.class);
		RecommendationResultCsvReader csvReader = mock(RecommendationResultCsvReader.class);
		RecommendationImportRowValidator validator = mock(RecommendationImportRowValidator.class);
		TransactionOperations txOps = passthroughTxOps();
		RestaurantRecommendationModel createdModel = RestaurantRecommendationModel.loading("deepfm-1");
		ReflectionTestUtils.setField(createdModel, "id", 1L);
		BatchExecution execution = BatchExecution.start(
			com.tasteam.domain.batch.entity.BatchType.RECOMMENDATION_IMPORT_ON_DEMAND,
			Instant.parse("2026-03-03T09:00:00Z"));
		RestaurantRecommendationRow row = new RestaurantRecommendationRow(
			1L,
			null,
			11L,
			0.91,
			1,
			"{}",
			"deepfm-1",
			Instant.parse("2026-03-03T10:00:00Z"),
			Instant.parse("2026-03-04T10:00:00Z"));

		when(repository.findByVersion("deepfm-1")).thenReturn(Optional.empty());
		when(repository.saveAndFlush(any(RestaurantRecommendationModel.class))).thenReturn(createdModel);
		when(repository.save(createdModel)).thenReturn(createdModel);
		when(batchExecutionRepository.save(any(BatchExecution.class))).thenReturn(execution);
		when(objectReader.openStream("s3://bucket/result.csv")).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(validator.validateAndConvertOrNull(any(ParsedRecommendationCsvRow.class), eq("deepfm-1"))).thenReturn(row);
		when(jdbcRepository.batchInsert(eq(1L), any())).thenReturn(1);
		RecommendationResultImportServiceImpl service = new RecommendationResultImportServiceImpl(
			batchExecutionRepository,
			repository,
			jdbcRepository,
			objectReader,
			csvReader,
			validator,
			txOps,
			new SimpleMeterRegistry());

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
				Instant.parse("2026-03-03T10:00:00Z"),
				Instant.parse("2026-03-04T10:00:00Z")));
			return null;
		}).when(csvReader).read(any(), any());

		RecommendationResultImportResult result = service.importResults(
			new RecommendationResultImportRequest("deepfm-1", "s3://bucket/result.csv", "req-1"));

		assertThat(result.modelVersion()).isEqualTo("deepfm-1");
		assertThat(result.insertedRows()).isEqualTo(1);
		assertThat(createdModel.getStatus()).isEqualTo(RestaurantRecommendationModelStatus.READY);
		verify(repository).saveAndFlush(any(RestaurantRecommendationModel.class));
		verify(repository, times(2)).save(createdModel);
	}

	@Test
	@DisplayName("모델이 존재하면 LOADING 후 READY 상태로 전이하고 결과를 반환한다")
	void ingest_marksLoadingThenReady() throws Exception {
		BatchExecutionRepository batchExecutionRepository = mock(BatchExecutionRepository.class);
		RestaurantRecommendationModelRepository repository = mock(RestaurantRecommendationModelRepository.class);
		RestaurantRecommendationJdbcRepository jdbcRepository = mock(RestaurantRecommendationJdbcRepository.class);
		RecommendationResultObjectReader objectReader = mock(RecommendationResultObjectReader.class);
		RecommendationResultCsvReader csvReader = mock(RecommendationResultCsvReader.class);
		RecommendationImportRowValidator validator = mock(RecommendationImportRowValidator.class);
		TransactionOperations txOps = passthroughTxOps();
		RestaurantRecommendationModel model = RestaurantRecommendationModel.loading("deepfm-1");
		ReflectionTestUtils.setField(model, "id", 1L);
		BatchExecution execution = BatchExecution.start(
			com.tasteam.domain.batch.entity.BatchType.RECOMMENDATION_IMPORT_ON_DEMAND,
			java.time.Instant.parse("2026-03-03T09:00:00Z"));
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

		when(repository.findByVersion("deepfm-1")).thenReturn(Optional.of(model));
		when(repository.save(model)).thenReturn(model);
		when(batchExecutionRepository.save(any(BatchExecution.class))).thenReturn(execution);
		when(objectReader.openStream("s3://bucket/result.csv")).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(validator.validateAndConvertOrNull(any(ParsedRecommendationCsvRow.class), eq("deepfm-1"))).thenReturn(row);
		when(jdbcRepository.batchInsert(eq(1L), any())).thenReturn(1);
		RecommendationResultImportServiceImpl service = new RecommendationResultImportServiceImpl(
			batchExecutionRepository,
			repository,
			jdbcRepository,
			objectReader,
			csvReader,
			validator,
			txOps,
			new SimpleMeterRegistry());

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
		verify(repository).findByVersion("deepfm-1");
		verify(repository, times(2)).save(model);
		verify(jdbcRepository).deleteByModelId(1L);
		verify(batchExecutionRepository, times(2)).save(any(BatchExecution.class));
	}

	@Test
	@DisplayName("동시에 같은 모델이 생성되면 다시 조회한 모델로 import를 진행한다")
	void importResults_reusesModelWhenCreateRaceOccurs() throws Exception {
		BatchExecutionRepository batchExecutionRepository = mock(BatchExecutionRepository.class);
		RestaurantRecommendationModelRepository repository = mock(RestaurantRecommendationModelRepository.class);
		RestaurantRecommendationJdbcRepository jdbcRepository = mock(RestaurantRecommendationJdbcRepository.class);
		RecommendationResultObjectReader objectReader = mock(RecommendationResultObjectReader.class);
		RecommendationResultCsvReader csvReader = mock(RecommendationResultCsvReader.class);
		RecommendationImportRowValidator validator = mock(RecommendationImportRowValidator.class);
		TransactionOperations txOps = passthroughTxOps();
		RestaurantRecommendationModel existingModel = RestaurantRecommendationModel.loading("deepfm-1");
		ReflectionTestUtils.setField(existingModel, "id", 3L);
		BatchExecution execution = BatchExecution.start(
			com.tasteam.domain.batch.entity.BatchType.RECOMMENDATION_IMPORT_ON_DEMAND,
			Instant.parse("2026-03-03T09:00:00Z"));
		RestaurantRecommendationRow row = new RestaurantRecommendationRow(
			1L,
			null,
			11L,
			0.91,
			1,
			"{}",
			"deepfm-1",
			Instant.parse("2026-03-03T10:00:00Z"),
			Instant.parse("2026-03-04T10:00:00Z"));

		when(repository.findByVersion("deepfm-1"))
			.thenReturn(Optional.empty())
			.thenReturn(Optional.of(existingModel));
		when(repository.saveAndFlush(any(RestaurantRecommendationModel.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate"));
		when(repository.save(existingModel)).thenReturn(existingModel);
		when(batchExecutionRepository.save(any(BatchExecution.class))).thenReturn(execution);
		when(objectReader.openStream("s3://bucket/result.csv")).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(validator.validateAndConvertOrNull(any(ParsedRecommendationCsvRow.class), eq("deepfm-1"))).thenReturn(row);
		when(jdbcRepository.batchInsert(eq(3L), any())).thenReturn(1);
		RecommendationResultImportServiceImpl service = new RecommendationResultImportServiceImpl(
			batchExecutionRepository,
			repository,
			jdbcRepository,
			objectReader,
			csvReader,
			validator,
			txOps,
			new SimpleMeterRegistry());

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
				Instant.parse("2026-03-03T10:00:00Z"),
				Instant.parse("2026-03-04T10:00:00Z")));
			return null;
		}).when(csvReader).read(any(), any());

		RecommendationResultImportResult result = service.importResults(
			new RecommendationResultImportRequest("deepfm-1", "s3://bucket/result.csv", "req-1"));

		assertThat(result.modelVersion()).isEqualTo("deepfm-1");
		assertThat(existingModel.getStatus()).isEqualTo(RestaurantRecommendationModelStatus.READY);
		verify(repository).saveAndFlush(any(RestaurantRecommendationModel.class));
		verify(repository, times(2)).findByVersion("deepfm-1");
		verify(jdbcRepository).deleteByModelId(3L);
	}

	private TransactionOperations passthroughTxOps() {
		TransactionOperations txOps = mock(TransactionOperations.class);
		when(txOps.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(null);
		});
		return txOps;
	}

	@Test
	@DisplayName("파일 IO 예외는 RESULT_IO_ERROR로 변환한다")
	void importResults_throwsResultIoErrorWhenIOException() throws Exception {
		BatchExecutionRepository batchExecutionRepository = mock(BatchExecutionRepository.class);
		RestaurantRecommendationModelRepository repository = mock(RestaurantRecommendationModelRepository.class);
		RestaurantRecommendationJdbcRepository jdbcRepository = mock(RestaurantRecommendationJdbcRepository.class);
		RecommendationResultObjectReader objectReader = mock(RecommendationResultObjectReader.class);
		RecommendationResultCsvReader csvReader = mock(RecommendationResultCsvReader.class);
		RecommendationImportRowValidator validator = mock(RecommendationImportRowValidator.class);
		TransactionOperations txOps = passthroughTxOps();
		RestaurantRecommendationModel model = RestaurantRecommendationModel.loading("deepfm-1");
		ReflectionTestUtils.setField(model, "id", 1L);
		BatchExecution execution = BatchExecution.start(
			com.tasteam.domain.batch.entity.BatchType.RECOMMENDATION_IMPORT_ON_DEMAND,
			java.time.Instant.parse("2026-03-03T09:00:00Z"));

		when(repository.findByVersion("deepfm-1")).thenReturn(Optional.of(model));
		when(repository.save(model)).thenReturn(model);
		when(batchExecutionRepository.save(any(BatchExecution.class))).thenReturn(execution);
		when(objectReader.openStream("s3://bucket/result.csv")).thenReturn(new ByteArrayInputStream(new byte[0]));
		doAnswer(invocation -> {
			throw new IOException("network");
		}).when(csvReader).read(any(), any());
		RecommendationResultImportServiceImpl service = new RecommendationResultImportServiceImpl(
			batchExecutionRepository,
			repository,
			jdbcRepository,
			objectReader,
			csvReader,
			validator,
			txOps,
			new SimpleMeterRegistry());

		assertThatThrownBy(() -> service.importResults(
			new RecommendationResultImportRequest("deepfm-1", "s3://bucket/result.csv", "req-1")))
			.isInstanceOf(RecommendationBusinessException.class)
			.extracting(ex -> ((RecommendationBusinessException)ex).getErrorCode())
			.isEqualTo(RecommendationErrorCode.RECOMMENDATION_RESULT_IO_ERROR.name());
	}
}
