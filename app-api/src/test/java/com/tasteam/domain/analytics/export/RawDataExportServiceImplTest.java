package com.tasteam.domain.analytics.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;
import com.tasteam.infra.storage.StorageClient;

@UnitTest
@DisplayName("[유닛](Analytics) RawDataExportServiceImpl 단위 테스트")
class RawDataExportServiceImplTest {

	@Test
	@DisplayName("경로/파일명 규약으로 CSV와 _SUCCESS를 업로드한다")
	void export_uploadsCsvAndSuccessMarkerWithContractPath() {
		RawDataExportSourceJdbcRepository sourceRepository = mock(RawDataExportSourceJdbcRepository.class);
		StorageClient storageClient = mock(StorageClient.class);
		BatchExecutionRepository batchExecutionRepository = mock(BatchExecutionRepository.class);
		RawDataExportServiceImpl service = new RawDataExportServiceImpl(sourceRepository, storageClient,
			batchExecutionRepository);
		LocalDate dt = LocalDate.of(2026, 3, 11);
		String prefix = "raw/restaurants/dt=2026-03-11/";
		String dataKey = prefix + "part-00001.csv";
		String successKey = prefix + "_SUCCESS";

		when(sourceRepository.restaurantHeaders()).thenReturn(List.of("restaurant_id", "restaurant_name"));
		doAnswer(invocation -> {
			CsvRowConsumer consumer = invocation.getArgument(0);
			consumer.accept(List.of("1", "식당A"));
			return null;
		}).when(sourceRepository).streamRestaurants(any(CsvRowConsumer.class));
		when(storageClient.listObjects(prefix)).thenReturn(List.of());
		when(batchExecutionRepository.save(any(BatchExecution.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		RawDataExportResult result = service.export(
			new RawDataExportCommand(dt, EnumSet.of(RawDataType.RESTAURANTS), "req-1"));

		assertThat(result.items()).hasSize(1);
		RawDataExportItemResult item = result.items().getFirst();
		assertThat(item.dataObjectKey()).isEqualTo(dataKey);
		assertThat(item.successObjectKey()).isEqualTo(successKey);
		assertThat(item.replacedExisting()).isFalse();

		InOrder inOrder = inOrder(storageClient);
		inOrder.verify(storageClient).deleteObject(successKey);
		inOrder.verify(storageClient).uploadObject(
			argThat(key -> key.endsWith("part-00001.csv")),
			any(java.nio.file.Path.class),
			anyString());
		inOrder.verify(storageClient).uploadObject(
			argThat(key -> key.equals(successKey)),
			any(byte[].class),
			anyString());
		verify(storageClient, never()).uploadObject(eq(dataKey), any(byte[].class), anyString());
	}

	@Test
	@DisplayName("동일 dt 재실행 시 REPLACE로 처리되고 replacedExisting=true를 반환한다")
	void export_sameDtRerunMarksReplacedExisting() {
		RawDataExportSourceJdbcRepository sourceRepository = mock(RawDataExportSourceJdbcRepository.class);
		StorageClient storageClient = mock(StorageClient.class);
		BatchExecutionRepository batchExecutionRepository = mock(BatchExecutionRepository.class);
		RawDataExportServiceImpl service = new RawDataExportServiceImpl(sourceRepository, storageClient,
			batchExecutionRepository);
		LocalDate dt = LocalDate.of(2026, 3, 11);
		String prefix = "raw/menus/dt=2026-03-11/";

		when(sourceRepository.menuHeaders()).thenReturn(List.of("restaurant_id", "menu_count"));
		doAnswer(invocation -> {
			CsvRowConsumer consumer = invocation.getArgument(0);
			consumer.accept(List.of("1", "2"));
			return null;
		}).when(sourceRepository).streamMenus(any(CsvRowConsumer.class));
		when(storageClient.listObjects(prefix)).thenReturn(List.of(prefix + "part-00001.csv", prefix + "_SUCCESS"));
		when(batchExecutionRepository.save(any(BatchExecution.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		RawDataExportResult result = service
			.export(new RawDataExportCommand(dt, EnumSet.of(RawDataType.MENUS), "req-2"));

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().replacedExisting()).isTrue();
	}

	@Test
	@DisplayName("업로드 실패 시 _SUCCESS를 정리하여 완료 마커가 남지 않게 한다")
	void export_cleanupSuccessMarkerWhenUploadFails() {
		RawDataExportSourceJdbcRepository sourceRepository = mock(RawDataExportSourceJdbcRepository.class);
		StorageClient storageClient = mock(StorageClient.class);
		BatchExecutionRepository batchExecutionRepository = mock(BatchExecutionRepository.class);
		RawDataExportServiceImpl service = new RawDataExportServiceImpl(sourceRepository, storageClient,
			batchExecutionRepository);
		LocalDate dt = LocalDate.of(2026, 3, 11);
		String prefix = "raw/restaurants/dt=2026-03-11/";
		String successKey = prefix + "_SUCCESS";

		when(sourceRepository.restaurantHeaders()).thenReturn(List.of("restaurant_id", "restaurant_name"));
		doAnswer(invocation -> {
			CsvRowConsumer consumer = invocation.getArgument(0);
			consumer.accept(List.of("1", "식당A"));
			return null;
		}).when(sourceRepository).streamRestaurants(any(CsvRowConsumer.class));
		when(storageClient.listObjects(prefix)).thenReturn(List.of());
		when(batchExecutionRepository.save(any(BatchExecution.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		doThrow(new RuntimeException("upload failed"))
			.when(storageClient).uploadObject(anyString(), any(java.nio.file.Path.class), anyString());

		assertThatThrownBy(() -> service.export(new RawDataExportCommand(
			dt, EnumSet.of(RawDataType.RESTAURANTS), "req-3")))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("upload failed");

		verify(storageClient, times(2)).deleteObject(successKey);
	}

	@Test
	@DisplayName("대량 row 스트리밍에서도 rowCount를 유지하며 경로 기반 업로드로 완료한다")
	void export_streamingLargeRows_keepsRowCountAndPathUpload() {
		RawDataExportSourceJdbcRepository sourceRepository = mock(RawDataExportSourceJdbcRepository.class);
		StorageClient storageClient = mock(StorageClient.class);
		BatchExecutionRepository batchExecutionRepository = mock(BatchExecutionRepository.class);
		RawDataExportServiceImpl service = new RawDataExportServiceImpl(sourceRepository, storageClient,
			batchExecutionRepository);
		LocalDate dt = LocalDate.of(2026, 3, 11);
		String prefix = "raw/menus/dt=2026-03-11/";
		String dataKey = prefix + "part-00001.csv";
		String successKey = prefix + "_SUCCESS";
		int largeRowCount = 20_000;

		when(sourceRepository.menuHeaders()).thenReturn(List.of("restaurant_id", "menu_count"));
		doAnswer(invocation -> {
			CsvRowConsumer consumer = invocation.getArgument(0);
			for (int i = 1; i <= largeRowCount; i++) {
				consumer.accept(List.of(String.valueOf(i), "2"));
			}
			return null;
		}).when(sourceRepository).streamMenus(any(CsvRowConsumer.class));
		when(storageClient.listObjects(prefix)).thenReturn(List.of());
		when(batchExecutionRepository.save(any(BatchExecution.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		RawDataExportResult result = service
			.export(new RawDataExportCommand(dt, EnumSet.of(RawDataType.MENUS), "req-large"));

		assertThat(result.items()).hasSize(1);
		RawDataExportItemResult item = result.items().getFirst();
		assertThat(item.rowCount()).isEqualTo(largeRowCount);
		assertThat(item.dataObjectKey()).isEqualTo(dataKey);
		assertThat(item.successObjectKey()).isEqualTo(successKey);

		verify(storageClient).uploadObject(eq(dataKey), any(java.nio.file.Path.class), eq("text/csv"));
		verify(storageClient, never()).uploadObject(eq(dataKey), any(byte[].class), anyString());
		verify(storageClient).uploadObject(eq(successKey), any(byte[].class), eq("text/plain"));
	}
}
