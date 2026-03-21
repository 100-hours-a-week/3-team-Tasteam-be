package com.tasteam.domain.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;

import com.tasteam.config.annotation.PerformanceTest;
import com.tasteam.domain.favorite.repository.MemberFavoriteRestaurantRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.fixture.MemberFixture;

@PerformanceTest
@DisplayName("[성능] FavoriteService 동시성 테스트")
class FavoriteConcurrencyPerformanceTest {

	@Autowired
	private FavoriteService favoriteService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MemberFavoriteRestaurantRepository memberFavoriteRepository;

	@Test
	@DisplayName("동시에 같은 내 찜을 등록해도 최종 활성 상태는 1건으로 일관된다")
	void concurrentCreateMyFavorite_keepsSingleActiveRow() throws Exception {
		Member concurrentMember = memberRepository.save(MemberFixture.create("concurrent@example.com", "동시성유저"));
		Restaurant concurrentRestaurant = restaurantRepository.save(createRestaurant("동시성식당"));

		int threadCount = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Callable<Boolean>> tasks = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			tasks.add(() -> {
				ready.countDown();
				start.await();
				try {
					favoriteService.createMyFavorite(concurrentMember.getId(), concurrentRestaurant.getId());
					return true;
				} catch (Exception ex) {
					return false;
				}
			});
		}

		List<Future<Boolean>> futures = new ArrayList<>();
		try {
			for (Callable<Boolean> task : tasks) {
				futures.add(executor.submit(task));
			}
			ready.await();
			start.countDown();
		} finally {
			executor.shutdown();
		}

		long successCount = 0;
		for (Future<Boolean> future : futures) {
			if (future.get()) {
				successCount++;
			}
		}

		assertThat(successCount).isGreaterThanOrEqualTo(1L);
		assertThat(memberFavoriteRepository.countByMemberIdAndRestaurantIdAndDeletedAtIsNull(concurrentMember.getId(),
			concurrentRestaurant.getId())).isEqualTo(1L);
	}

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"서울시 강남구 테헤란로 456",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-1111-2222");
	}
}
