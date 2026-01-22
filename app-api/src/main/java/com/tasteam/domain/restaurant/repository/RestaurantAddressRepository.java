package com.tasteam.domain.restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.RestaurantAddress;

public interface RestaurantAddressRepository extends JpaRepository<RestaurantAddress, Long> {

	Optional<RestaurantAddress> findByRestaurantId(Long restaurantId);
}
