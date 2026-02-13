package com.tasteam.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.chat.entity.ChatMessageFile;

public interface ChatMessageFileRepository extends JpaRepository<ChatMessageFile, Long> {}
