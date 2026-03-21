package com.tasteam.domain.restaurant.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantImageService {

	private final FileService fileService;

	@Transactional(readOnly = true)
	public Optional<RestaurantPrimaryImage> getPrimaryImage(Long restaurantId) {
		Map<Long, List<DomainImageItem>> restaurantImages = fileService.getDomainImageUrls(
			DomainType.RESTAURANT,
			List.of(restaurantId));

		List<DomainImageItem> images = restaurantImages.get(restaurantId);
		if (images == null || images.isEmpty()) {
			return Optional.empty();
		}

		DomainImageItem first = images.getFirst();
		return Optional.of(new RestaurantPrimaryImage(first.imageId(), first.url()));
	}

	@Transactional(readOnly = true)
	public Map<Long, List<RestaurantImageDto>> getRestaurantThumbnails(List<Long> restaurantIds, int limit) {
		Map<Long, List<DomainImageItem>> domainImages = fileService.getDomainImageUrls(
			DomainType.RESTAURANT,
			restaurantIds);

		return domainImages.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> convertAndLimit(entry.getValue(), limit)));
	}

	private List<RestaurantImageDto> convertAndLimit(List<DomainImageItem> images, int limit) {
		if (images == null || images.isEmpty()) {
			return List.of();
		}

		List<DomainImageItem> limited = images.size() > limit ? images.subList(0, limit) : images;
		return limited.stream()
			.map(image -> new RestaurantImageDto(image.imageId(), image.url()))
			.toList();
	}

	public record RestaurantPrimaryImage(Long imageId, String url) {
	}
}
