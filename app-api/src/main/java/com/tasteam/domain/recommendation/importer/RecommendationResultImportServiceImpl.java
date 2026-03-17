package com.tasteam.domain.recommendation.importer;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

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
	private final MeterRegistry meterRegistry;

	public RecommendationResultImportServiceImpl(
		BatchExecutionRepository batchExecutionRepository,
		RestaurantRecommendationModelRepository modelRepository,
		RestaurantRecommendationJdbcRepository recommendationJdbcRepository,
		RecommendationResultObjectReader resultObjectReader,
		RecommendationResultCsvReader csvReader,
		RecommendationImportRowValidator rowValidator,
		TransactionOperations transactionOperations,
		MeterRegistry meterRegistry) {
		this.batchExecutionRepository = batchExecutionRepository;
		this.modelRepository = modelRepository;
		this.recommendationJdbcRepository = recommendationJdbcRepository;
		this.resultObjectReader = resultObjectReader;
		this.csvReader = csvReader;
		this.rowValidator = rowValidator;
		this.transactionOperations = transactionOperations;
		this.meterRegistry = meterRegistry;
	}

	@Override
	public RecommendationResultImportResult importResults(RecommendationResultImportRequest request) {
		Objects.requireNonNull(request, "request는 null일 수 없습니다.");
		Timer.Sample totalSample = Timer.start(meterRegistry);

		RestaurantRecommendationModel model = findOrCreateLoadingModel(request.requestedModelVersion());
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
				accumulator.markLastLineNumber(parsedRow.lineNumber());
				accumulator.incrementTotalRows();
				try {
					RestaurantRecommendationRow row = rowValidator.validateAndConvertOrNull(
						parsedRow,
						request.requestedModelVersion());
					if (row == null) {
						accumulator.incrementSkippedRows();
						return;
					}
					buffer.add(row);
					if (buffer.size() >= INSERT_CHUNK_SIZE) {
						flushChunk(modelId, buffer, accumulator);
					}
				} catch (RuntimeException ex) {
					accumulator.incrementFailedRows();
					throw ex;
				}
			});

			if (!buffer.isEmpty()) {
				flushChunk(modelId, buffer, accumulator);
			}

			accumulator.reconcileFailureCount();
			accumulator.assertInvariant();
			markReady(model);
			finishBatchExecution(batchExecution, accumulator, BatchExecutionStatus.COMPLETED);
			recordRowSummary(accumulator);
			recordCounter("recommendation.import.execute.total", "stage", "import", "result", "success");
			recordTimer("recommendation.import.execute.duration", totalSample, "stage", "import", "result", "success");
			log.info(
				"recommendation import completed. batchExecutionId={}, modelVersion={}, requestId={}, totalRows={}, insertedRows={}, skippedRows={}, failedRows={}",
				batchExecution.getId(),
				model.getVersion(),
				request.requestId(),
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows,
				accumulator.failedRows);
			return new RecommendationResultImportResult(
				model.getVersion(),
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows);
		} catch (IOException ex) {
			accumulator.reconcileFailureCount();
			markFailed(model);
			finishBatchExecution(batchExecution, accumulator, BatchExecutionStatus.FAILED);
			recordRowSummary(accumulator);
			recordCounter("recommendation.import.execute.total", "stage", "import", "result", "io_failed");
			recordTimer("recommendation.import.execute.duration", totalSample, "stage", "import", "result",
				"io_failed");
			log.error(
				"recommendation import failed by io. batchExecutionId={}, modelVersion={}, requestId={}, lineNumber={}, totalRows={}, insertedRows={}, skippedRows={}, failedRows={}, errorCode={}, message={}",
				batchExecution.getId(),
				model.getVersion(),
				request.requestId(),
				accumulator.lastLineNumber,
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows,
				accumulator.failedRows,
				"RECOMMENDATION_RESULT_IO_ERROR",
				ex.getMessage(),
				ex);
			throw RecommendationBusinessException.resultIoError("추천 결과 파일을 읽는 중 오류가 발생했습니다: " + ex.getMessage());
		} catch (RuntimeException ex) {
			accumulator.reconcileFailureCount();
			markFailed(model);
			finishBatchExecution(batchExecution, accumulator, BatchExecutionStatus.FAILED);
			recordRowSummary(accumulator);
			recordCounter("recommendation.import.execute.total", "stage", "import", "result", "failed");
			recordTimer("recommendation.import.execute.duration", totalSample, "stage", "import", "result", "failed");
			log.error(
				"recommendation import failed. batchExecutionId={}, modelVersion={}, requestId={}, lineNumber={}, totalRows={}, insertedRows={}, skippedRows={}, failedRows={}, errorCode={}, message={}",
				batchExecution.getId(),
				model.getVersion(),
				request.requestId(),
				accumulator.lastLineNumber,
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows,
				accumulator.failedRows,
				resolveErrorCode(ex),
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

	private RestaurantRecommendationModel findOrCreateLoadingModel(String version) {
		return modelRepository.findByVersion(version)
			.orElseGet(() -> createLoadingModel(version));
	}

	private RestaurantRecommendationModel createLoadingModel(String version) {
		try {
			RestaurantRecommendationModel created = transactionOperations
				.execute(status -> modelRepository.saveAndFlush(RestaurantRecommendationModel.loading(version)));
			if (created == null) {
				throw RecommendationBusinessException.resultValidationFailed(
					"추천 모델 생성 결과가 비어 있습니다. version=" + version);
			}
			return created;
		} catch (DataIntegrityViolationException ex) {
			return modelRepository.findByVersion(version)
				.orElseThrow(() -> RecommendationBusinessException.resultValidationFailed(
					"추천 모델 생성 중 중복 충돌이 발생했지만 모델을 다시 찾지 못했습니다. version=" + version));
		}
	}

	private void flushChunk(long modelId, List<RestaurantRecommendationRow> buffer, ImportAccumulator accumulator) {
		try {
			accumulator.addInsertedRows(flushBuffer(modelId, buffer));
		} catch (RuntimeException ex) {
			accumulator.addFailedRows(buffer.size());
			throw ex;
		} finally {
			buffer.clear();
		}
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

	private void finishBatchExecution(BatchExecution execution, ImportAccumulator accumulator,
		BatchExecutionStatus status) {
		accumulator.reconcileFailureCount();
		execution.finish(
			Instant.now(),
			(int)accumulator.totalRows,
			(int)accumulator.insertedRows,
			(int)accumulator.failedRows,
			(int)accumulator.skippedRows,
			status);
		batchExecutionRepository.save(execution);
	}

	private String resolveErrorCode(Throwable throwable) {
		if (throwable instanceof RecommendationBusinessException recommendationBusinessException) {
			return recommendationBusinessException.getErrorCode();
		}
		return throwable.getClass().getSimpleName();
	}

	private void recordRowSummary(ImportAccumulator accumulator) {
		recordSummary("recommendation.import.rows.total", accumulator.totalRows);
		recordSummary("recommendation.import.rows.inserted", accumulator.insertedRows);
		recordSummary("recommendation.import.rows.skipped", accumulator.skippedRows);
		recordSummary("recommendation.import.rows.failed", accumulator.failedRows);
	}

	private void recordSummary(String metricName, long value) {
		DistributionSummary.builder(metricName)
			.register(meterRegistry)
			.record(value);
	}

	private void recordCounter(String metricName, String... tags) {
		MetricLabelPolicy.validate(metricName, tags);
		meterRegistry.counter(metricName, tags).increment();
	}

	private void recordTimer(String metricName, Timer.Sample sample, String... tags) {
		MetricLabelPolicy.validate(metricName, tags);
		sample.stop(Timer.builder(metricName).tags(tags).register(meterRegistry));
	}

	private static final class ImportAccumulator {

		private long totalRows;
		private long insertedRows;
		private long skippedRows;
		private long failedRows;
		private long lastLineNumber;

		private void incrementTotalRows() {
			totalRows += 1;
		}

		private void addInsertedRows(long count) {
			insertedRows += count;
		}

		private void incrementSkippedRows() {
			skippedRows += 1;
		}

		private void incrementFailedRows() {
			failedRows += 1;
		}

		private void addFailedRows(long count) {
			failedRows += Math.max(0L, count);
		}

		private void markLastLineNumber(long lineNumber) {
			lastLineNumber = lineNumber;
		}

		private void reconcileFailureCount() {
			long derived = Math.max(0L, totalRows - insertedRows - skippedRows);
			if (failedRows < derived) {
				failedRows = derived;
			}
		}

		private void assertInvariant() {
			if (totalRows != insertedRows + skippedRows + failedRows) {
				throw RecommendationBusinessException.resultValidationFailed(
					"집계 불변식 위반: totalRows != insertedRows + skippedRows + failedRows");
			}
		}
	}
}
