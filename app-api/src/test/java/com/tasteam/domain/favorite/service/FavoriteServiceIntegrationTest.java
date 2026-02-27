package com.tasteam.domain.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.favorite.dto.response.FavoriteCreateResponse;
import com.tasteam.domain.favorite.dto.response.RestaurantFavoriteTargetItem;
import com.tasteam.domain.favorite.dto.response.RestaurantFavoriteTargetsResponse;
import com.tasteam.domain.favorite.dto.response.SubgroupFavoriteRestaurantItem;
import com.tasteam.domain.favorite.entity.MemberFavoriteRestaurant;
import com.tasteam.domain.favorite.entity.SubgroupFavoriteRestaurant;
import com.tasteam.domain.favorite.repository.MemberFavoriteRestaurantRepository;
import com.tasteam.domain.favorite.repository.SubgroupFavoriteRestaurantRepository;
import com.tasteam.domain.favorite.type.FavoriteState;
import com.tasteam.domain.favorite.type.FavoriteTargetType;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.fixture.SubgroupFixture;
import com.tasteam.fixture.SubgroupMemberFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FavoriteErrorCode;

@ServiceIntegrationTest
@Transactional
class FavoriteServiceIntegrationTest {

	@Autowired
	private FavoriteService favoriteService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private SubgroupRepository subgroupRepository;

	@Autowired
	private SubgroupMemberRepository subgroupMemberRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MemberFavoriteRestaurantRepository memberFavoriteRepository;

	@Autowired
	private SubgroupFavoriteRestaurantRepository subgroupFavoriteRepository;

	private Member member;
	private Group group;
	private Subgroup subgroup;
	private Restaurant restaurant;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(MemberFixture.create());
		group = groupRepository.save(GroupFixture.create());
		subgroup = subgroupRepository.save(SubgroupFixture.create(group, "테스트 소모임"));
		subgroupMemberRepository.save(SubgroupMemberFixture.create(subgroup.getId(), member));
		restaurant = restaurantRepository.save(createRestaurant("테스트 식당"));
	}

	@Test
	@DisplayName("내 찜 등록은 소프트 삭제된 데이터를 복구한다")
	void createMyFavorite_restoresDeletedFavorite() {
		MemberFavoriteRestaurant deleted = memberFavoriteRepository.save(
			MemberFavoriteRestaurant.create(member.getId(), restaurant.getId()));
		deleted.delete();

		FavoriteCreateResponse response = favoriteService.createMyFavorite(member.getId(), restaurant.getId());

		assertThat(response.id()).isEqualTo(deleted.getId());
		assertThat(memberFavoriteRepository.findByMemberIdAndRestaurantIdAndDeletedAtIsNull(member.getId(),
			restaurant.getId())).isPresent();
	}

	@Test
	@DisplayName("내 찜 중복 등록 시 FAVORITE_ALREADY_EXISTS 예외가 발생한다")
	void createMyFavorite_whenDuplicated_throwsConflict() {
		favoriteService.createMyFavorite(member.getId(), restaurant.getId());

		assertThatThrownBy(() -> favoriteService.createMyFavorite(member.getId(), restaurant.getId()))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode()).isEqualTo(
				FavoriteErrorCode.FAVORITE_ALREADY_EXISTS.name()));
	}

	@Test
	@DisplayName("찜 타겟 조회는 내 찜과 소모임 찜 타겟을 함께 반환한다")
	void getFavoriteTargets_returnsMyAndSubgroupTargets() {
		favoriteService.createMyFavorite(member.getId(), restaurant.getId());
		subgroupFavoriteRepository.save(SubgroupFavoriteRestaurant.create(member.getId(), subgroup.getId(),
			restaurant.getId()));

		RestaurantFavoriteTargetsResponse response = favoriteService.getFavoriteTargets(member.getId(),
			restaurant.getId());

		assertThat(response.targets()).hasSize(2);
		RestaurantFavoriteTargetItem myTarget = response.targets().stream()
			.filter(t -> t.targetType() == FavoriteTargetType.ME)
			.findFirst()
			.orElseThrow();
		RestaurantFavoriteTargetItem subgroupTarget = response.targets().stream()
			.filter(t -> t.targetType() == FavoriteTargetType.SUBGROUP)
			.findFirst()
			.orElseThrow();

		assertThat(myTarget.favoriteState()).isEqualTo(FavoriteState.FAVORITED);
		assertThat(subgroupTarget.targetId()).isEqualTo(subgroup.getId());
		assertThat(subgroupTarget.favoriteState()).isEqualTo(FavoriteState.FAVORITED);
	}

	@Test
	@DisplayName("소모임 찜 등록 후 삭제하면 데이터가 제거된다")
	void createAndDeleteSubgroupFavorite() {
		FavoriteCreateResponse created = favoriteService.createSubgroupFavorite(member.getId(), subgroup.getId(),
			restaurant.getId());

		assertThat(created.id()).isNotNull();
		assertThat(subgroupFavoriteRepository.findBySubgroupIdAndRestaurantId(subgroup.getId(),
			restaurant.getId())).isPresent();

		favoriteService.deleteSubgroupFavorite(member.getId(), subgroup.getId(), restaurant.getId());

		assertThat(subgroupFavoriteRepository.findBySubgroupIdAndRestaurantId(subgroup.getId(),
			restaurant.getId())).get().extracting(SubgroupFavoriteRestaurant::getDeletedAt).isNotNull();
	}

	@Test
	@DisplayName("소모임 찜 삭제는 생성한 회원만 가능하다")
	void deleteSubgroupFavorite_onlyOwnerCanDelete() {
		Member another = memberRepository.save(MemberFixture.create("another@example.com", "다른유저"));
		SubgroupFavoriteRestaurant favorite = subgroupFavoriteRepository.save(
			SubgroupFavoriteRestaurant.create(another.getId(), subgroup.getId(), restaurant.getId()));

		assertThatThrownBy(() -> favoriteService.deleteSubgroupFavorite(member.getId(), subgroup.getId(),
			restaurant.getId()))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode()).isEqualTo(
				CommonErrorCode.NO_PERMISSION.name()));

		assertThat(favorite.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("내 찜 삭제는 멱등하게 동작한다")
	void deleteMyFavorite_isIdempotent() {
		favoriteService.deleteMyFavorite(member.getId(), restaurant.getId());
		favoriteService.deleteMyFavorite(member.getId(), restaurant.getId());

		assertThat(memberFavoriteRepository.findByMemberIdAndRestaurantIdAndDeletedAtIsNull(member.getId(),
			restaurant.getId())).isEmpty();
	}

	@Test
	@DisplayName("공개 소모임은 비회원도 찜 목록 조회가 가능하다")
	void getSubgroupFavorites_openSubgroup_allowsNonMember() {
		Member nonMember = memberRepository.save(MemberFixture.create("viewer@example.com", "조회유저"));

		CursorPageResponse<SubgroupFavoriteRestaurantItem> response = favoriteService.getSubgroupFavoriteRestaurants(
			nonMember.getId(), subgroup.getId(), null);

		assertThat(response).isNotNull();
	}

	@Test
	@DisplayName("비공개 소모임은 비회원 찜 목록 조회를 금지한다")
	void getSubgroupFavorites_privateSubgroup_deniesNonMember() {
		Group privateGroup = groupRepository.save(GroupFixture.create("비공개그룹", "서울특별시"));
		Subgroup privateSubgroup = subgroupRepository.save(
			SubgroupFixture.create(privateGroup, "비공개 소모임", SubgroupJoinType.PASSWORD, 0));
		Member nonMember = memberRepository.save(MemberFixture.create("viewer2@example.com", "비회원"));

		assertThatThrownBy(() -> favoriteService.getSubgroupFavoriteRestaurants(nonMember.getId(),
			privateSubgroup.getId(), null))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode()).isEqualTo(
				CommonErrorCode.NO_PERMISSION.name()));
	}

	@Test
	@DisplayName("소모임 찜 목록 조회는 같은 음식점을 restaurantId 기준으로 1건만 반환한다")
	void getSubgroupFavorites_deduplicatesByRestaurantId() {
		Member another = memberRepository.save(MemberFixture.create("duplicate@example.com", "중복유저"));
		subgroupFavoriteRepository.save(SubgroupFavoriteRestaurant.create(member.getId(), subgroup.getId(),
			restaurant.getId()));
		subgroupFavoriteRepository.save(SubgroupFavoriteRestaurant.create(another.getId(), subgroup.getId(),
			restaurant.getId()));

		CursorPageResponse<SubgroupFavoriteRestaurantItem> response = favoriteService.getSubgroupFavoriteRestaurants(
			member.getId(),
			subgroup.getId(),
			null);

		assertThat(response.items()).hasSize(1);
		assertThat(response.items().getFirst().restaurantId()).isEqualTo(restaurant.getId());
	}

	@Test
	@DisplayName("소모임 찜 목록은 같은 소모임의 찜 멤버 수를 restaurant별로 집계해 반환한다")
	void getSubgroupFavorites_returnsGroupFavoriteMemberCount() {
		Subgroup anotherSubgroupInSameGroup = subgroupRepository.save(SubgroupFixture.create(group, "같은 그룹 소모임"));
		Member another = memberRepository.save(MemberFixture.create("count@example.com", "카운트유저"));

		subgroupFavoriteRepository.save(SubgroupFavoriteRestaurant.create(member.getId(), subgroup.getId(),
			restaurant.getId()));
		subgroupFavoriteRepository.save(SubgroupFavoriteRestaurant.create(member.getId(), anotherSubgroupInSameGroup
			.getId(), restaurant.getId()));
		subgroupFavoriteRepository.save(SubgroupFavoriteRestaurant.create(another.getId(), anotherSubgroupInSameGroup
			.getId(), restaurant.getId()));

		CursorPageResponse<SubgroupFavoriteRestaurantItem> response = favoriteService.getSubgroupFavoriteRestaurants(
			member.getId(),
			subgroup.getId(),
			null);

		assertThat(response.items()).hasSize(1);
		assertThat(response.items().getFirst().groupFavoriteCount()).isEqualTo(1L);
	}

	@Test
	@Disabled("TODO: 트랜잭션 경계/DB 격리수준을 반영한 안정적인 동시성 테스트로 재작성 필요")
	@DisplayName("동시에 같은 내 찜을 등록해도 최종 활성 상태는 1건으로 일관된다")
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
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
				} catch (BusinessException ex) {
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
