package com.tasteam.batch.ai.report;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.event.BatchExecutionFinishedEvent;
import com.tasteam.domain.batch.dto.JobTypeStatusCount;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.AiJobType;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.infra.webhook.discord.DiscordMessage;

@Component
public class BatchReportDiscordMessageFactory {

	private static final int COLOR_COMPLETED = 3066993;
	private static final int COLOR_FAILED = 15158332;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter STARTED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withZone(KST);

	public DiscordMessage create(BatchExecutionFinishedEvent event, List<JobTypeStatusCount> byTypeStatusCounts) {
		boolean completed = event.status() == BatchExecutionStatus.COMPLETED;
		String title = completed ? "BATCH COMPLETED" : "BATCH FAILED";
		int color = completed ? COLOR_COMPLETED : COLOR_FAILED;

		List<DiscordMessage.Field> fields = new ArrayList<>();
		fields.add(new DiscordMessage.Field("📋 Execution", "#" + event.batchExecutionId(), true));
		fields.add(new DiscordMessage.Field("🕐 Duration", formatDuration(event), true));
		fields.add(new DiscordMessage.Field("🕐 Started At", STARTED_AT_FORMATTER.format(event.startedAt()), true));
		fields.add(new DiscordMessage.Field("📊 Summary", buildSummary(event), false));
		fields.add(new DiscordMessage.Field("📌 By Job Type", buildByJobType(byTypeStatusCounts), false));

		DiscordMessage.Embed embed = new DiscordMessage.Embed(
			title,
			null,
			color,
			fields,
			event.finishedAt().toString(),
			new DiscordMessage.Footer("AI Batch System"));

		return new DiscordMessage(List.of(embed));
	}

	private String formatDuration(BatchExecutionFinishedEvent event) {
		long seconds = Math.max(0, Duration.between(event.startedAt(), event.finishedAt()).toSeconds());
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long remain = seconds % 60;

		if (hours > 0) {
			return String.format("%dh %dm %ds", hours, minutes, remain);
		}
		if (minutes > 0) {
			return String.format("%dm %ds", minutes, remain);
		}
		return String.format("%ds", remain);
	}

	private String buildSummary(BatchExecutionFinishedEvent event) {
		String header = "Total   ✅ Success   ❌ Failed   ⚠️ Stale";
		String row = String.format("%5d %11d %10d %10d",
			event.totalJobs(), event.successCount(), event.failedCount(), event.staleCount());
		return "```\n" + header + "\n" + row + "\n```";
	}

	private String buildByJobType(List<JobTypeStatusCount> counts) {
		Map<AiJobType, TypeCount> byType = new EnumMap<>(AiJobType.class);
		for (AiJobType jobType : AiJobType.values()) {
			byType.put(jobType, new TypeCount());
		}

		for (JobTypeStatusCount item : counts) {
			TypeCount typeCount = byType.get(item.jobType());
			if (typeCount == null) {
				continue;
			}
			typeCount.total += item.count();
			if (item.status() == AiJobStatus.COMPLETED) {
				typeCount.success += item.count();
			} else if (item.status() == AiJobStatus.FAILED) {
				typeCount.failed += item.count();
			} else if (item.status() == AiJobStatus.STALE) {
				typeCount.stale += item.count();
			}
		}

		StringBuilder body = new StringBuilder();
		body.append("```\n");
		body.append("Job Type                Total  ✅   ❌   ⚠️\n");
		for (AiJobType jobType : AiJobType.values()) {
			TypeCount c = byType.get(jobType);
			body.append(String.format("%-24s %5d %3d %3d %3d%n",
				jobType.name(), c.total, c.success, c.failed, c.stale));
		}
		body.append("```");
		return body.toString();
	}

	private static final class TypeCount {
		private long total;
		private long success;
		private long failed;
		private long stale;
	}
}
