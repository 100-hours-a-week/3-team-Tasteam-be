package com.tasteam.domain.file.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.Image;

public interface DomainImageRepository extends JpaRepository<DomainImage, Long> {

	Optional<DomainImage> findByDomainTypeAndDomainIdAndImage(DomainType domainType, Long domainId, Image image);

	List<DomainImage> findAllByImage(Image image);

	@Query("""
		select di from DomainImage di
		join fetch di.image i
		where di.domainType = :domainType
		  and di.domainId in :domainIds
		  and i.status = 'ACTIVE'
		order by di.domainId asc, di.sortOrder asc
		""")
	List<DomainImage> findAllByDomainTypeAndDomainIdIn(
		@Param("domainType")
		DomainType domainType,
		@Param("domainIds")
		List<Long> domainIds);

	List<DomainImage> findAllByDomainTypeAndDomainId(DomainType domainType, Long domainId);

	void deleteAllByDomainTypeAndDomainId(DomainType domainType, Long domainId);
}
