package com.tasteam.domain.file.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;

public interface ImageRepository extends JpaRepository<Image, Long> {

	Optional<Image> findByFileUuid(UUID fileUuid);

	Optional<Image> findByFileUuidAndStatus(UUID fileUuid, ImageStatus status);

	List<Image> findAllByFileUuidIn(List<UUID> fileUuids);

	List<Image> findAllByStatusAndDeletedAtIsNotNull(ImageStatus status);

	List<Image> findAllByStatusAndDeletedAtIsNullAndCreatedAtBefore(ImageStatus status, Instant cutoff);
}
