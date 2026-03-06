package com.tasteam.batch.dummy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.batch.dummy.DummySeedJobTracker;
import com.tasteam.batch.dummy.repository.DummyDataJdbcRepository;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;
import com.tasteam.global.exception.business.BusinessException;

@UnitTest
@DisplayName("[유닛](Dummy) DummyDataSeedService 파생 제약 검증 테스트")
class DummyDataSeedServiceValidationTest {

	@Mock
	private DummyDataJdbcRepository dummyRepo;

	@Mock
	private DummySeedJobTracker tracker;

	@InjectMocks
	private DummyDataSeedService service;

	@Test
	@DisplayName("favoriteCount가 memberCount × restaurantCount를 넘으면 INVALID_REQUEST를 반환한다")
	void favoriteCount_초과_예외() {
		// given
		var request = new AdminDummySeedRequest(
			2,
			1,
			0,
			0,
			0,
			0,
			0,
			0,
			5,
			0);

		// when
		Throwable throwable = catchThrowable(() -> service.seed(request));

		// then
		assertThat(throwable)
			.isInstanceOf(BusinessException.class)
			.extracting(t -> ((BusinessException)t).getErrorCode())
			.isEqualTo("INVALID_REQUEST");
		verifyNoInteractions(dummyRepo);
	}

	@Test
	@DisplayName("subgroupFavoriteCount가 member × subgroup × restaurant 조합을 넘으면 INVALID_REQUEST를 반환한다")
	void subgroupFavoriteCount_초과_예외() {
		// given
		var request = new AdminDummySeedRequest(
			2,
			1,
			1,
			2,
			1,
			0,
			0,
			0,
			0,
			10);

		// when
		Throwable throwable = catchThrowable(() -> service.seed(request));

		// then
		assertThat(throwable)
			.isInstanceOf(BusinessException.class)
			.extracting(t -> ((BusinessException)t).getErrorCode())
			.isEqualTo("INVALID_REQUEST");
		verifyNoInteractions(dummyRepo);
	}

	@Test
	@DisplayName("chatMessagePerRoom가 group×subgroup 조합 한도를 넘으면 INVALID_REQUEST를 반환한다")
	void chatMessagePerRoom_과도_예외() {
		// given
		var request = new AdminDummySeedRequest(
			1,
			1,
			1000,
			1000,
			1,
			0,
			25000,
			0,
			1,
			1);

		// when
		Throwable throwable = catchThrowable(() -> service.seed(request));

		// then
		assertThat(throwable)
			.isInstanceOf(BusinessException.class)
			.extracting(t -> ((BusinessException)t).getErrorCode())
			.isEqualTo("INVALID_REQUEST");
		verifyNoInteractions(dummyRepo);
	}

	@Test
	@DisplayName("groupCount×subgroupPerGroup가 상한을 넘으면 INVALID_REQUEST를 반환한다")
	void 그룹_서브그룹조합_과도_예외() {
		// given
		var request = new AdminDummySeedRequest(
			1,
			1,
			1001,
			1000,
			1,
			0,
			0,
			0,
			1,
			0);

		// when
		Throwable throwable = catchThrowable(() -> service.seed(request));

		// then
		assertThat(throwable)
			.isInstanceOf(BusinessException.class)
			.extracting(t -> ((BusinessException)t).getErrorCode())
			.isEqualTo("INVALID_REQUEST");
		verifyNoInteractions(dummyRepo);
	}
}
