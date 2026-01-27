package com.tasteam.domain.search.repository;

import java.util.List;

import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;

public interface SearchQueryRepository {

	List<Restaurant> searchRestaurantsByKeyword(String keyword, SearchCursor cursor, int size);
}
