package com.workflowpro.repository;

import com.workflowpro.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
    long countByRecipientIdAndIsReadFalse(Long recipientId);
}
