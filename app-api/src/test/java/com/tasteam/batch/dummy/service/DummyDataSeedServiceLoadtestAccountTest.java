package com.tasteam.batch.dummy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.batch.dummy.DummySeedJobTracker;
import com.tasteam.batch.dummy.repository.DummyDataJdbcRepository;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;

@UnitTest
@DisplayName("[유닛](Dummy) DummyDataSeedService 부하테스트 계정 매핑 테스트")
class DummyDataSeedServiceLoadtestAccountTest {

	@Mock
	private DummyDataJdbcRepository dummyRepo;

	@Mock
	private DummySeedJobTracker tracker;

	@InjectMocks
	private DummyDataSeedService service;

	@Test
	@DisplayName("더미 멤버를 시딩하면 부하테스트 식별자를 가진 TEST OAuth 계정을 함께 생성한다")
	void seed_createsMembersWithLoadtestIdentifiers() {
		// given
		given(tracker.isCancelRequested()).willReturn(false);
		given(dummyRepo.insertMembersWithTestOAuthAccounts(anyList(), anyList(), anyList()))
			.willAnswer(invocation -> {
				List<String> emails = invocation.getArgument(0);
				List<String> nicknames = invocation.getArgument(1);
				List<String> identifiers = invocation.getArgument(2);

				assertThat(emails).hasSize(3);
				assertThat(emails).allMatch(email -> email.endsWith("@dummy.tasteam.kr"));
				assertThat(nicknames).hasSize(3);
				assertThat(nicknames.get(0)).startsWith("더미유저-").endsWith("-1");
				assertThat(nicknames.get(1)).startsWith("더미유저-").endsWith("-2");
				assertThat(nicknames.get(2)).startsWith("더미유저-").endsWith("-3");
				assertThat(identifiers).containsExactly("test-user-001", "test-user-002", "test-user-003");
				throw new IllegalStateException("member insert stop");
			});
		AdminDummySeedRequest request = new AdminDummySeedRequest(3, 1, 1, 1, 1, 1, 1, 1, 1, 1);

		// when & then
		assertThatThrownBy(() -> service.seed(request))
			.isInstanceOf(DummyDataSeedService.DummySeedStepException.class)
			.hasMessageContaining("member insert");
		verify(dummyRepo).insertMembersWithTestOAuthAccounts(anyList(), anyList(), anyList());
	}
}
