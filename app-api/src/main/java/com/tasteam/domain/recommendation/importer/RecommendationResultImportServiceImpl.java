package com.tasteam.domain.recommendation.importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModel;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationJdbcRepository;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationRow;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationModelRepository;

@Service
public class RecommendationResultImportServiceImpl implements RecommendationResultImportService {

	private static final int INSERT_CHUNK_SIZE = 5_000;

	private final RestaurantRecommendationModelRepository modelRepository;
	private final RestaurantRecommendationJdbcRepository recommendationJdbcRepository;
	private final RecommendationResultObjectReader resultObjectReader;
	private final RecommendationResultCsvReader csvReader;
	private final RecommendationImportRowValidator rowValidator;
	private final TransactionOperations transactionOperations;

	public RecommendationResultImportServiceImpl(
		RestaurantRecommendationModelRepository modelRepository,
		RestaurantRecommendationJdbcRepository recommendationJdbcRepository,
		RecommendationResultObjectReader resultObjectReader,
		RecommendationResultCsvReader csvReader,
		RecommendationImportRowValidator rowValidator,
		TransactionOperations transactionOperations) {
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

		RestaurantRecommendationModel model = modelRepository.findById(request.requestedModelVersion())
			.orElseThrow(() -> RecommendationBusinessException.modelNotFound(request.requestedModelVersion()));

		markLoading(model);

		try (InputStream inputStream = resultObjectReader.openStream(request.s3Uri())) {
			deleteExistingRows(model.getVersion());

			ImportAccumulator accumulator = new ImportAccumulator();
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
					accumulator.addInsertedRows(flushBuffer(model.getVersion(), buffer));
					buffer.clear();
				}
			});

			if (!buffer.isEmpty()) {
				accumulator.addInsertedRows(flushBuffer(model.getVersion(), buffer));
				buffer.clear();
			}

			markReady(model);
			return new RecommendationResultImportResult(
				model.getVersion(),
				accumulator.totalRows,
				accumulator.insertedRows,
				accumulator.skippedRows);
		} catch (IOException ex) {
			markFailed(model);
			throw RecommendationBusinessException.csvFormatInvalid("추천 결과 파일을 읽는 중 오류가 발생했습니다: " + ex.getMessage());
		} catch (RuntimeException ex) {
			markFailed(model);
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

	private int flushBuffer(String modelVersion, List<RestaurantRecommendationRow> buffer) {
		Integer inserted = transactionOperations.execute(
			status -> recommendationJdbcRepository.batchInsert(buffer));
		return inserted == null ? 0 : inserted;
	}

	private void deleteExistingRows(String modelVersion) {
		transactionOperations.execute(status -> {
			recommendationJdbcRepository.deleteByPipelineVersion(modelVersion);
			return null;
		});
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
