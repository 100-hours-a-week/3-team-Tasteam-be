package com.tasteam.infra.messagequeue.trace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageQueueTraceLogRepository extends JpaRepository<MessageQueueTraceLog, Long> {

	Page<MessageQueueTraceLog> findAllByMessageIdOrderByIdDesc(String messageId, Pageable pageable);

	Page<MessageQueueTraceLog> findAllByOrderByIdDesc(Pageable pageable);
}
