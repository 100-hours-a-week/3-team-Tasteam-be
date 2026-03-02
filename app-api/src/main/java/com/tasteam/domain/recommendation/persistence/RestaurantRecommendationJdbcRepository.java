package com.tasteam.domain.recommendation.persistence;

import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RestaurantRecommendationJdbcRepository {

	private static final String DELETE_BY_MODEL_SQL = """
		DELETE FROM restaurant_recommendation
		WHERE model_id = ?
		""";

	private static final String COUNT_BY_MODEL_SQL = """
		SELECT COUNT(*)
		FROM restaurant_recommendation
		WHERE model_id = ?
		""";

	private static final String INSERT_SQL = """
		INSERT INTO restaurant_recommendation (
			user_id,
			restaurant_id,
			score,
			rank,
			model_id,
			generated_at,
			expires_at
		) VALUES (?, ?, ?, ?, ?, ?, ?)
		""";

	private static final String FIND_BY_USER_AND_MODEL_SQL = """
		SELECT user_id, restaurant_id, score, rank, generated_at, expires_at
		FROM restaurant_recommendation
		WHERE user_id = ?
		  AND model_id = ?
		ORDER BY rank ASC
		LIMIT ?
		""";

	private final JdbcTemplate jdbcTemplate;

	public int deleteByModelId(String modelId) {
		return jdbcTemplate.update(DELETE_BY_MODEL_SQL, modelId);
	}

	public long countByModelId(String modelId) {
		Long count = jdbcTemplate.queryForObject(COUNT_BY_MODEL_SQL, Long.class, modelId);
		return count == null ? 0L : count;
	}

	public int batchInsert(String modelId, List<RestaurantRecommendationRow> rows) {
		Assert.hasText(modelId, "modelId는 비어 있을 수 없습니다.");
		if (rows == null || rows.isEmpty()) {
			return 0;
		}
		int[][] result = jdbcTemplate.batchUpdate(
			INSERT_SQL,
			rows,
			rows.size(),
			(ps, row) -> {
				ps.setLong(1, row.userId());
				ps.setLong(2, row.restaurantId());
				ps.setDouble(3, row.score());
				ps.setInt(4, row.rank());
				ps.setString(5, modelId);
				ps.setObject(6, row.generatedAt());
				ps.setObject(7, row.expiresAt());
			});
		int inserted = 0;
		for (int[] chunk : result) {
			for (int rowResult : chunk) {
				if (rowResult > 0) {
					inserted += rowResult;
				}
			}
		}
		return inserted;
	}

	public List<RestaurantRecommendationRow> findTopByUserIdAndModelId(long userId, String modelId, int limit) {
		if (limit <= 0) {
			return Collections.emptyList();
		}
		return jdbcTemplate.query(
			FIND_BY_USER_AND_MODEL_SQL,
			(rs, rowNum) -> new RestaurantRecommendationRow(
				rs.getLong("user_id"),
				rs.getLong("restaurant_id"),
				rs.getDouble("score"),
				rs.getInt("rank"),
				rs.getObject("generated_at", java.time.OffsetDateTime.class).toInstant(),
				rs.getObject("expires_at", java.time.OffsetDateTime.class).toInstant()),
			userId,
			modelId,
			limit);
	}
}
