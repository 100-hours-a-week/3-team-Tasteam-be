package com.tasteam.domain.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.favorite.dto.response.FavoriteRestaurantItem;
import com.tasteam.domain.favorite.entity.MemberFavoriteRestaurant;
import com.tasteam.domain.favorite.repository.MemberFavoriteRestaurantRepository;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;

@ServiceIntegrationTest
@Transactional
class FavoriteRestaurantImageIntegrationTest {

	@Autowired
	private FavoriteService favoriteService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MemberFavoriteRestaurantRepository favoriteRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	private Member member;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(MemberFixture.create());
	}

	@Nested
	@DisplayName("즐겹 목록 조회 시 이미지 해석")
	class GetMyFavoriteRestaurants {

		@Test
		@DisplayName("이미지가 있는 즐겨찾기 음식점은 thumbnailUrl이 반환된다")
		void getFavorites_withImage_returnsThumbnailUrl() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("이미지 있는 식당"));
			favoriteRepository.save(MemberFavoriteRestaurant.create(member.getId(), restaurant.getId()));

			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(ImageFixture.create(FilePurpose.RESTAURANT_IMAGE, "restaurants/fav-thumb.png",
				fileUuid, "fav-thumb.png"));
			var image = imageRepository.findByFileUuid(fileUuid).orElseThrow();
			image.activate();
			domainImageRepository.save(DomainImage.create(DomainType.RESTAURANT, restaurant.getId(), image, 0));

			CursorPageResponse<FavoriteRestaurantItem> response = favoriteService
				.getMyFavoriteRestaurants(member.getId(), null);

			assertThat(response.items()).hasSize(1);
			assertThat(response.items().getFirst().thumbnailUrl()).isNotNull();
			assertThat(response.items().getFirst().thumbnailUrl()).contains("fav-thumb.png");
		}

		@Test
		@DisplayName("이미지가 없는 즐겨찾기 음식점은 thumbnailUrl이 null이다")
		void getFavorites_withoutImage_returnsThumbnailUrlNull() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("이미지 없는 식당"));
			favoriteRepository.save(MemberFavoriteRestaurant.create(member.getId(), restaurant.getId()));

			CursorPageResponse<FavoriteRestaurantItem> response = favoriteService
				.getMyFavoriteRestaurants(member.getId(), null);

			assertThat(response.items()).hasSize(1);
			assertThat(response.items().getFirst().thumbnailUrl()).isNull();
		}

		@Test
		@DisplayName("즐겨찾기가 비어있으면 빈 목록이 반환된다")
		void getFavorites_empty_returnsEmptyList() {
			CursorPageResponse<FavoriteRestaurantItem> response = favoriteService
				.getMyFavoriteRestaurants(member.getId(), null);

			assertThat(response.items()).isEmpty();
		}
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
