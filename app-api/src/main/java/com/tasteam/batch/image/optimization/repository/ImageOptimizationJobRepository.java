package com.tasteam.batch.image.optimization.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tasteam.batch.image.optimization.entity.ImageOptimizationJob;
import com.tasteam.domain.file.entity.Image;

public interface ImageOptimizationJobRepository extends JpaRepository<ImageOptimizationJob, Long> {

	boolean existsByImageId(Long imageId);

	@Query("SELECT i FROM Image i WHERE i.status = 'ACTIVE' AND i.id NOT IN (SELECT j.image.id FROM ImageOptimizationJob j)")
	List<Image> findUnoptimizedImages(Pageable pageable);
}
