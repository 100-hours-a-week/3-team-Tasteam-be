package com.tasteam.domain.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.chat.entity.ChatMessageFile;

public interface ChatMessageFileRepository extends JpaRepository<ChatMessageFile, Long> {
	List<ChatMessageFile> findAllByChatMessageIdInAndDeletedAtIsNull(List<Long> chatMessageIds);
}
