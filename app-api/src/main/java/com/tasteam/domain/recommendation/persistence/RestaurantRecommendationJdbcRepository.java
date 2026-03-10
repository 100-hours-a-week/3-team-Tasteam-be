package com.tasteam.domain.recommendation.persistence;

import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
			member_id,
			anonymous_id,
			restaurant_id,
			score,
			rank,
			model_id,
			context_snapshot,
			pipeline_version,
			generated_at,
			expires_at
		) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)
		""";

	private static final String FIND_BY_USER_AND_MODEL_SQL = """
		SELECT member_id, anonymous_id, restaurant_id, score, rank, context_snapshot::text AS context_snapshot, pipeline_version, generated_at, expires_at
		FROM restaurant_recommendation
		WHERE member_id = ?
		  AND model_id = ?
		ORDER BY rank ASC
		LIMIT ?
		""";

	private static final String FIND_BY_ANONYMOUS_AND_MODEL_SQL = """
		SELECT member_id, anonymous_id, restaurant_id, score, rank, context_snapshot::text AS context_snapshot, pipeline_version, generated_at, expires_at
		FROM restaurant_recommendation
		WHERE anonymous_id = ?
		  AND model_id = ?
		ORDER BY rank ASC
		LIMIT ?
		""";

	private final JdbcTemplate jdbcTemplate;

	public int deleteByModelId(long modelId) {
		return jdbcTemplate.update(DELETE_BY_MODEL_SQL, modelId);
	}

	public long countByModelId(long modelId) {
		Long count = jdbcTemplate.queryForObject(COUNT_BY_MODEL_SQL, Long.class, modelId);
		return count == null ? 0L : count;
	}

	public int batchInsert(long modelId, List<RestaurantRecommendationRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return 0;
		}
		int[][] result = jdbcTemplate.batchUpdate(
			INSERT_SQL,
			rows,
			rows.size(),
			(ps, row) -> {
				if (row.memberId() == null) {
					ps.setNull(1, java.sql.Types.BIGINT);
				} else {
					ps.setLong(1, row.memberId());
				}
				ps.setString(2, row.anonymousId());
				ps.setLong(3, row.restaurantId());
				ps.setDouble(4, row.score());
				ps.setInt(5, row.rank());
				ps.setLong(6, modelId);
				ps.setString(7, row.contextSnapshot());
				ps.setString(8, row.pipelineVersion());
				ps.setObject(9, row.generatedAt());
				ps.setObject(10, row.expiresAt());
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

	public List<RestaurantRecommendationRow> findTopByMemberIdAndModelId(long memberId, long modelId,
		int limit) {
		if (limit <= 0) {
			return Collections.emptyList();
		}
		return jdbcTemplate.query(
			FIND_BY_USER_AND_MODEL_SQL,
			(rs, rowNum) -> new RestaurantRecommendationRow(
				rs.getObject("member_id", Long.class),
				rs.getString("anonymous_id"),
				rs.getLong("restaurant_id"),
				rs.getDouble("score"),
				rs.getInt("rank"),
				rs.getString("context_snapshot"),
				rs.getString("pipeline_version"),
				rs.getObject("generated_at", java.time.OffsetDateTime.class).toInstant(),
				rs.getObject("expires_at", java.time.OffsetDateTime.class).toInstant()),
			memberId,
			modelId,
			limit);
	}

	public List<RestaurantRecommendationRow> findTopByAnonymousIdAndModelId(String anonymousId, long modelId,
		int limit) {
		if (limit <= 0) {
			return Collections.emptyList();
		}
		return jdbcTemplate.query(
			FIND_BY_ANONYMOUS_AND_MODEL_SQL,
			(rs, rowNum) -> new RestaurantRecommendationRow(
				rs.getObject("member_id", Long.class),
				rs.getString("anonymous_id"),
				rs.getLong("restaurant_id"),
				rs.getDouble("score"),
				rs.getInt("rank"),
				rs.getString("context_snapshot"),
				rs.getString("pipeline_version"),
				rs.getObject("generated_at", java.time.OffsetDateTime.class).toInstant(),
				rs.getObject("expires_at", java.time.OffsetDateTime.class).toInstant()),
			anonymousId,
			modelId,
			limit);
	}
}
