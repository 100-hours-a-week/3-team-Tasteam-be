package com.tasteam.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tasteam.domain.chat.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByIdAndDeletedAtIsNull(Long id);

	Optional<ChatRoom> findBySubgroupIdAndDeletedAtIsNull(Long subgroupId);

	@Query("select c.id from ChatRoom c where c.deletedAt is null")
	List<Long> findActiveRoomIds();
}
