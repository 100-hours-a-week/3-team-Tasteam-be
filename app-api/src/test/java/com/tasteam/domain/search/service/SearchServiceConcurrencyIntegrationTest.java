package com.tasteam.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;

import com.tasteam.config.annotation.PerformanceTest;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.search.dto.request.SearchRequest;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.MemberFixture;

@PerformanceTest
class SearchServiceConcurrencyIntegrationTest {

	private static final int THREAD_COUNT = 100;

	@Autowired
	private SearchService searchService;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberSearchHistoryRepository searchHistoryRepository;

	private ExecutorService executorService;

	@BeforeEach
	void setUp() {
		executorService = Executors.newFixedThreadPool(THREAD_COUNT);
	}

	@AfterEach
	void tearDown() throws InterruptedException {
		if (executorService != null) {
			executorService.shutdown();
			executorService.awaitTermination(10, TimeUnit.SECONDS);
		}

		searchHistoryRepository.deleteAll();
		memberRepository.deleteAll();
		restaurantRepository.deleteAll();
		groupRepository.deleteAll();
	}

	@Nested
	@DisplayName("여러 사용자가 동시에 각기 다른 검색어로 검색")
	class MultipleUsersWithDifferentKeywords {

		@Test
		@DisplayName("100명의 사용자가 동시에 100개의 서로 다른 검색어로 검색하면 모든 검색 히스토리가 기록된다")
		void multipleUsersSearchWithDifferentKeywordsConcurrently() throws InterruptedException {
			List<Member> members = new ArrayList<>();
			for (int i = 0; i < THREAD_COUNT; i++) {
				Member member = memberRepository.save(MemberFixture.create("user" + i + "@example.com", "user" + i));
				members.add(member);
			}

			List<String> keywords = new ArrayList<>();
			for (int i = 0; i < THREAD_COUNT; i++) {
				String keyword = "검색어" + i;
				keywords.add(keyword);
				groupRepository.save(GroupFixture.create(keyword + " 모임", "서울특별시 강남구"));
				restaurantRepository.save(createRestaurant(keyword + " 식당"));
			}

			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
			AtomicInteger successCount = new AtomicInteger(0);

			for (int i = 0; i < THREAD_COUNT; i++) {
				int index = i;
				executorService.submit(() -> {
					try {
						startLatch.await();

						SearchResponse response = searchService.search(
							members.get(index).getId(),
							new SearchRequest(keywords.get(index), null, null, null, null, 10));

						if (!response.groups().isEmpty() || !response.restaurants().items().isEmpty()) {
							successCount.incrementAndGet();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			endLatch.await(30, TimeUnit.SECONDS);

			assertThat(successCount.get()).isEqualTo(THREAD_COUNT);

			Thread.sleep(2000);

			long historyCount = searchHistoryRepository.count();
			assertThat(historyCount).isEqualTo(THREAD_COUNT);
		}
	}

	@Nested
	@DisplayName("한 사용자가 동시에 같은 검색어로 여러 번 검색")
	class SingleUserWithSameKeyword {

		@Test
		@DisplayName("한 사용자가 같은 검색어로 100번 동시 검색하면 검색 히스토리가 기록된다")
		void singleUserSearchesSameKeywordConcurrently() throws InterruptedException {
			Member member = memberRepository.save(MemberFixture.create("single@example.com", "single"));

			String keyword = "맛집";
			groupRepository.save(GroupFixture.create(keyword + " 모임", "서울특별시 강남구"));
			restaurantRepository.save(createRestaurant(keyword + " 식당"));

			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
			AtomicInteger successCount = new AtomicInteger(0);

			for (int i = 0; i < THREAD_COUNT; i++) {
				executorService.submit(() -> {
					try {
						startLatch.await();

						SearchResponse response = searchService.search(
							member.getId(),
							new SearchRequest(keyword, null, null, null, null, 10));

						if (!response.groups().isEmpty() || !response.restaurants().items().isEmpty()) {
							successCount.incrementAndGet();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			endLatch.await(30, TimeUnit.SECONDS);

			assertThat(successCount.get()).isEqualTo(THREAD_COUNT);

			Thread.sleep(2000);

			List<MemberSearchHistory> histories = searchHistoryRepository.findAllByMemberIdAndKeywordAndDeletedAtIsNull(
				member.getId(), keyword);

			assertThat(histories).isNotEmpty();
			long totalCount = histories.stream().mapToLong(MemberSearchHistory::getCount).sum();
			assertThat(totalCount).isGreaterThan(0L).isLessThanOrEqualTo((long)THREAD_COUNT);
		}
	}

	@Nested
	@DisplayName("여러 사용자가 동시에 같은 검색어로 검색")
	class MultipleUsersWithSameKeyword {

		@Test
		@DisplayName("100명의 사용자가 동시에 같은 검색어로 검색하면 각 사용자별 검색 히스토리가 기록된다")
		void multipleUsersSearchSameKeywordConcurrently() throws InterruptedException {
			List<Member> members = new ArrayList<>();
			for (int i = 0; i < THREAD_COUNT; i++) {
				Member member = memberRepository.save(MemberFixture.create("user" + i + "@example.com", "user" + i));
				members.add(member);
			}

			String keyword = "공통검색어";
			groupRepository.save(GroupFixture.create(keyword + " 모임", "서울특별시 강남구"));
			restaurantRepository.save(createRestaurant(keyword + " 식당"));

			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
			AtomicInteger successCount = new AtomicInteger(0);

			for (int i = 0; i < THREAD_COUNT; i++) {
				int index = i;
				executorService.submit(() -> {
					try {
						startLatch.await();

						SearchResponse response = searchService.search(
							members.get(index).getId(),
							new SearchRequest(keyword, null, null, null, null, 10));

						if (!response.groups().isEmpty() || !response.restaurants().items().isEmpty()) {
							successCount.incrementAndGet();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			endLatch.await(30, TimeUnit.SECONDS);

			assertThat(successCount.get()).isEqualTo(THREAD_COUNT);

			Thread.sleep(2000);

			long historyCount = searchHistoryRepository.count();
			assertThat(historyCount).isEqualTo(THREAD_COUNT);

			for (Member member : members) {
				List<MemberSearchHistory> histories = searchHistoryRepository
					.findAllByMemberIdAndKeywordAndDeletedAtIsNull(
						member.getId(), keyword);
				assertThat(histories).hasSize(1);
				assertThat(histories.get(0).getKeyword()).isEqualTo(keyword);
			}
		}
	}

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"서울특별시 강남구 테헤란로 123",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-7777-8888");
	}
}
