package com.tasteam.domain.announcement.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.announcement.entity.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

	Page<Announcement> findAllByDeletedAtIsNullOrderByIdDesc(Pageable pageable);

	Optional<Announcement> findByIdAndDeletedAtIsNull(Long id);
}
