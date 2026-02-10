package com.tasteam.domain.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.favorite.dto.response.FavoriteCreateResponse;
import com.tasteam.domain.favorite.dto.response.FavoriteTargetItem;
import com.tasteam.domain.favorite.dto.response.FavoriteTargetsResponse;
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
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
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
	private Subgroup subgroup;
	private Restaurant restaurant;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(MemberFixture.create());
		Group group = groupRepository.save(GroupFixture.create());
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

		FavoriteTargetsResponse response = favoriteService.getFavoriteTargets(member.getId(), restaurant.getId());

		assertThat(response.targets()).hasSize(2);
		FavoriteTargetItem myTarget = response.targets().stream()
			.filter(t -> t.targetType() == FavoriteTargetType.ME)
			.findFirst()
			.orElseThrow();
		FavoriteTargetItem subgroupTarget = response.targets().stream()
			.filter(t -> t.targetType() == FavoriteTargetType.SUBGROUP)
			.findFirst()
			.orElseThrow();

		assertThat(myTarget.favoriteCount()).isEqualTo(1L);
		assertThat(myTarget.favoriteState()).isEqualTo(FavoriteState.FAVORITED);
		assertThat(subgroupTarget.targetId()).isEqualTo(subgroup.getId());
		assertThat(subgroupTarget.favoriteCount()).isEqualTo(1L);
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

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"서울시 강남구 테헤란로 456",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-1111-2222");
	}
}
