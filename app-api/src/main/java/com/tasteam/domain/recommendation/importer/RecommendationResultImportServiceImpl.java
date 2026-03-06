package com.tasteam.domain.recommendation.importer;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;
import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModel;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationJdbcRepository;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationRow;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationModelRepository;

@Service
public class RecommendationResultImportServiceImpl implements RecommendationResultImportService {

	private static final int INSERT_CHUNK_SIZE = 5_000;
	private static final BatchType IMPORT_BATCH_TYPE = BatchType.RECOMMENDATION_IMPORT_ON_DEMAND;
	private static final Logger log = LoggerFactory.getLogger(RecommendationResultImportServiceImpl.class);

	private final BatchExecutionRepository batchExecutionRepository;
	private final RestaurantRecommendationModelRepository modelRepository;
	private final RestaurantRecommendationJdbcRepository recommendationJdbcRepository;
	private final RecommendationResultObjectReader resultObjectReader;
	private final RecommendationResultCsvReader csvReader;
	private final RecommendationImportRowValidator rowValidator;
	private final TransactionOperations transactionOperations;

	public RecommendationResultImportServiceImpl(
		BatchExecutionRepository batchExecutionRepository,
		RestaurantRecommendationModelRepository modelRepository,
		RestaurantRecommendationJdbcRepository recommendationJdbcRepository,
		RecommendationResultObjectReader resultObjectReader,
		RecommendationResultCsvReader csvReader,
		RecommendationImportRowValidator rowValidator,
		TransactionOperations transactionOperations) {
		this.batchExecutionRepository = batchExecutionRepository;
		this.modelRepository = modelRepository;
		this.recommendationJdbcRepository = recommendationJdbcRepository;
		this.resultObjectReader = resultObjectReader;
		this.csvReader = csvReader;
		this.rowValidator = rowValidator;
		this.transactionOperations = transactionOperations;
	}

	@Override
	public RecommendationResultImportResult importResults(RecommendationResultImportRequest request) {
		Objects.requireNonNull(request, "request는 null일 수 없습니다.");

		RestaurantRecommendationModel model = modelRepository.findByVersion(request.requestedModelVersion())
			.orElseThrow(() -> RecommendationBusinessException.modelNotFound(request.requestedModelVersion()));
		Long modelId = model.getId();
		if (modelId == null) {
			throw RecommendationBusinessException.resultValidationFailed(
				"추천 모델 ID가 존재하지 않습니다. version=" + model.getVersion());
		}

		BatchExecution batchExecution = startBatchExecution();
		markLoading(model);
		ImportAccumulator accumulator = new ImportAccumulator();

		try (InputStream inputStream = resultObjectReader.openStream(request.s3Uri())) {
			deleteExistingRows(modelId);

			List<RestaurantRecommendationRow> buffer = new ArrayList<>(INSERT_CHUNK_SIZE);

			csvReader.read(inputStream, parsedRow -> {
				accumulator.incrementTotalRows();
				RestaurantRecommendationRow row = rowValidator.validateAndConvertOrNull(
					parsedRow,
					request.requestedModelVersion());
				if (row == null) {
					accumulator.incrementSkippedRows();
					return;
				}
				buffer.add(row);
				if (buffer.size() >= INSERT_CHUNK_SIZE) {
					accumulator.addInsertedRows(flushBuffer(modelId, buffer));
					buffer.clear();
				}
			});

			if (!buffer.isEmpty()) {
				accumulator.addInsertedRows(flushBuffer(modelId, buffer));
				buffer.clear();
			}

			markReady(model);
			finishBatchExecutionSuccess(batchExecution, accumulator);
			log.info(
				"recommendation import completed. batchExecutionId={}, modelVersion={}, requestId={}, totalRows={}, insertedRows={}, skippedRows={}",
				batchExecution.getId(),
				model.getVersion(),
				request.requestId(),
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows);
			return new RecommendationResultImportResult(
				model.getVersion(),
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows);
		} catch (IOException ex) {
			markFailed(model);
			finishBatchExecutionFailed(batchExecution, accumulator);
			log.error(
				"recommendation import failed by io. batchExecutionId={}, modelVersion={}, requestId={}, totalRows={}, insertedRows={}, skippedRows={}, message={}",
				batchExecution.getId(),
				model.getVersion(),
				request.requestId(),
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows,
				ex.getMessage(),
				ex);
			throw RecommendationBusinessException.csvFormatInvalid("추천 결과 파일을 읽는 중 오류가 발생했습니다: " + ex.getMessage());
		} catch (RuntimeException ex) {
			markFailed(model);
			finishBatchExecutionFailed(batchExecution, accumulator);
			log.error(
				"recommendation import failed. batchExecutionId={}, modelVersion={}, requestId={}, totalRows={}, insertedRows={}, skippedRows={}, message={}",
				batchExecution.getId(),
				model.getVersion(),
				request.requestId(),
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows,
				ex.getMessage(),
				ex);
			throw ex;
		}
	}

	protected void markLoading(RestaurantRecommendationModel model) {
		transactionOperations.execute(status -> {
			model.markLoading();
			modelRepository.save(model);
			return null;
		});
	}

	protected void markReady(RestaurantRecommendationModel model) {
		transactionOperations.execute(status -> {
			model.markReady();
			modelRepository.save(model);
			return null;
		});
	}

	protected void markFailed(RestaurantRecommendationModel model) {
		transactionOperations.execute(status -> {
			model.markFailed();
			modelRepository.save(model);
			return null;
		});
	}

	private int flushBuffer(long modelId, List<RestaurantRecommendationRow> buffer) {
		Integer inserted = transactionOperations.execute(
			status -> recommendationJdbcRepository.batchInsert(modelId, buffer));
		return inserted == null ? 0 : inserted;
	}

	private void deleteExistingRows(long modelId) {
		transactionOperations.execute(status -> {
			recommendationJdbcRepository.deleteByModelId(modelId);
			return null;
		});
	}

	private BatchExecution startBatchExecution() {
		BatchExecution execution = BatchExecution.start(IMPORT_BATCH_TYPE, Instant.now());
		return batchExecutionRepository.save(execution);
	}

	private void finishBatchExecutionSuccess(BatchExecution execution, ImportAccumulator accumulator) {
		execution.finish(
			Instant.now(),
			(int)accumulator.totalRows,
			(int)accumulator.insertedRows,
			0,
			(int)accumulator.skippedRows,
			BatchExecutionStatus.COMPLETED);
		batchExecutionRepository.save(execution);
	}

	private void finishBatchExecutionFailed(BatchExecution execution, ImportAccumulator accumulator) {
		long failed = Math.max(0L, accumulator.totalRows - accumulator.insertedRows - accumulator.skippedRows);
		execution.finish(
			Instant.now(),
			(int)accumulator.totalRows,
			(int)accumulator.insertedRows,
			(int)failed,
			(int)accumulator.skippedRows,
			BatchExecutionStatus.FAILED);
		batchExecutionRepository.save(execution);
	}

	private static final class ImportAccumulator {

		private long totalRows;
		private long insertedRows;
		private long skippedRows;

		private void incrementTotalRows() {
			totalRows += 1;
		}

		private void addInsertedRows(long count) {
			insertedRows += count;
		}

		private void incrementSkippedRows() {
			skippedRows += 1;
		}
	}
}
