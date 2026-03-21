package com.tasteam.domain.analytics.export;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.LocalDate;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RawDataExportRuntimeDiagnosticsLogger implements RawDataExportRuntimeDiagnostics {

	private final DataSource dataSource;

	@Override
	public void logSnapshot(String phase, RawDataExportCommand command, LocalDate dt, int totalJobs, int successCount) {
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
		MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

		long heapUsedMb = toMiB(heap.getUsed());
		long heapMaxMb = toMiB(heap.getMax());
		long nonHeapUsedMb = toMiB(nonHeap.getUsed());
		int threadCount = threadMXBean.getThreadCount();
		int peakThreadCount = threadMXBean.getPeakThreadCount();

		Integer hikariActive = null;
		Integer hikariIdle = null;
		Integer hikariWaiting = null;
		Integer hikariTotal = null;

		HikariDataSource hikariDataSource = resolveHikariDataSource();
		if (hikariDataSource != null) {
			HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
			if (pool != null) {
				hikariActive = pool.getActiveConnections();
				hikariIdle = pool.getIdleConnections();
				hikariWaiting = pool.getThreadsAwaitingConnection();
				hikariTotal = pool.getTotalConnections();
			}
		}

		log.info(
			"raw export runtime snapshot. phase={}, requestId={}, dt={}, totalJobs={}, successCount={}, heapUsedMiB={}, heapMaxMiB={}, nonHeapUsedMiB={}, threadCount={}, peakThreadCount={}, hikariActive={}, hikariIdle={}, hikariWaiting={}, hikariTotal={}",
			phase,
			command == null ? null : command.requestId(),
			dt,
			totalJobs,
			successCount,
			heapUsedMb,
			heapMaxMb,
			nonHeapUsedMb,
			threadCount,
			peakThreadCount,
			hikariActive,
			hikariIdle,
			hikariWaiting,
			hikariTotal);
	}

	private HikariDataSource resolveHikariDataSource() {
		if (dataSource instanceof HikariDataSource hikariDataSource) {
			return hikariDataSource;
		}
		try {
			return dataSource.unwrap(HikariDataSource.class);
		} catch (Exception ignore) {
			return null;
		}
	}

	private long toMiB(long bytes) {
		if (bytes < 0) {
			return -1L;
		}
		return bytes / (1024 * 1024);
	}
}
