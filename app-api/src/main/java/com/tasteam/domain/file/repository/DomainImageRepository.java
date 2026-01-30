package com.tasteam.domain.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.Image;

public interface DomainImageRepository extends JpaRepository<DomainImage, Long> {

	Optional<DomainImage> findByDomainTypeAndDomainIdAndImage(DomainType domainType, Long domainId, Image image);

	List<DomainImage> findAllByImage(Image image);

	@Modifying
	@Transactional
	void deleteAllByDomainTypeAndDomainId(DomainType domainType, Long domainId);
}
