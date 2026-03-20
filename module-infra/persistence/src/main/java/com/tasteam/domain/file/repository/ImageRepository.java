package com.tasteam.domain.file.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;

public interface ImageRepository extends JpaRepository<Image, Long> {

	Optional<Image> findByFileUuid(UUID fileUuid);

	Optional<Image> findByFileUuidAndStatus(UUID fileUuid, ImageStatus status);

	List<Image> findAllByFileUuidIn(List<UUID> fileUuids);

	List<Image> findAllByStatusAndDeletedAtBefore(ImageStatus status, Instant cutoff);

	List<Image> findAllByStatusAndDeletedAtIsNullAndCreatedAtBefore(ImageStatus status, Instant cutoff);

	@Query("""
		SELECT i FROM Image i
		WHERE i.storageKey IN :storageKeys
		  AND i.status = 'ACTIVE'
		  AND i.deletedAt IS NULL
		  AND LOWER(i.fileType) <> 'image/webp'
		""")
	List<Image> findOptimizationCandidatesByStorageKeys(
		@Param("storageKeys")
		Collection<String> storageKeys);
}
